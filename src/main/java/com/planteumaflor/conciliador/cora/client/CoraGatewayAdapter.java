package com.planteumaflor.conciliador.cora.client;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.cora.application.CoraGateway;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Adapter real da API direta do Cora: OAuth client-credentials + mTLS por empresa.
 *
 * Cada empresa tem seu próprio certificado/chave, então o {@link SSLContext}
 * NÃO é um bean estático — é montado em memória a cada chamada a partir do PEM
 * já decifrado pelo chamador (JDK puro: CertificateFactory/KeyFactory/KeyStore,
 * sem dependência nova).
 *
 * ATENÇÃO: o schema JSON de saldo/extrato ({@link CoraDto}) é PROVISÓRIO —
 * confirmar contra o sandbox real do Cora antes de uso em produção.
 */
@Component
class CoraGatewayAdapter implements CoraGateway {

    private final String authBaseUrl;
    private final String apiBaseUrl;
    private final String tokenPath;
    private final String extratoPath;
    private final String saldoPath;

    CoraGatewayAdapter(ConciliadorProperties properties) {
        this.authBaseUrl = properties.cora().authBaseUrl();
        this.apiBaseUrl = properties.cora().apiBaseUrl();
        this.tokenPath = properties.cora().tokenPath();
        this.extratoPath = properties.cora().extratoPath();
        this.saldoPath = properties.cora().saldoPath();
    }

    @Override
    public BigDecimal saldo(CredenciaisCora credenciais) {
        String token = autenticar(credenciais);
        CoraDto.SaldoResponse resposta = restClient(credenciais, apiBaseUrl).get()
                .uri(saldoPath)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(CoraDto.SaldoResponse.class);
        return resposta == null || resposta.saldoDisponivel() == null
                ? BigDecimal.ZERO
                : resposta.saldoDisponivel();
    }

    @Override
    public List<LancamentoExtrato> extrato(CredenciaisCora credenciais, LocalDate de, LocalDate ate) {
        String token = autenticar(credenciais);
        RestClient client = restClient(credenciais, apiBaseUrl);
        List<LancamentoExtrato> lancamentos = new ArrayList<>();
        for (int pagina = 1; pagina <= 100; pagina++) {
            int paginaAtual = pagina;
            CoraDto.ExtratoResponse resposta = client.get()
                    .uri(uriBuilder -> uriBuilder.path(extratoPath)
                            .queryParam("start", de)
                            .queryParam("end", ate)
                            .queryParam("page", paginaAtual)
                            .queryParam("perPage", 100)
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(CoraDto.ExtratoResponse.class);

            if (resposta == null || resposta.lancamentos() == null || resposta.lancamentos().isEmpty()) {
                break;
            }
            for (CoraDto.Lancamento lancamento : resposta.lancamentos()) {
                if (lancamento.id() != null && lancamento.dataLancamento() != null && lancamento.valor() != null) {
                    lancamentos.add(new LancamentoExtrato(
                            lancamento.id(),
                            lancamento.contaId(),
                            lancamento.dataLancamento(),
                            lancamento.valor(),
                            lancamento.descricao(),
                            lancamento.documentoContraparte(),
                            lancamento.endToEndId()));
                }
            }
            if (!Boolean.TRUE.equals(resposta.hasMore()) && resposta.next() == null) {
                break;
            }
        }
        return lancamentos;
    }

    private String autenticar(CredenciaisCora credenciais) {
        CoraDto.TokenResponse resposta = restClient(credenciais, authBaseUrl).post()
                .uri(tokenPath)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("grant_type=client_credentials&client_id=" + credenciais.clientId())
                .retrieve()
                .body(CoraDto.TokenResponse.class);
        if (resposta == null || resposta.accessToken() == null) {
            throw new IllegalStateException("Cora não retornou access_token");
        }
        return resposta.accessToken();
    }

    private RestClient restClient(CredenciaisCora credenciais, String baseUrl) {
        SSLContext sslContext = montarSslContext(credenciais);
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    SSLContext montarSslContext(CredenciaisCora credenciais) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Certificate certificado = certFactory.generateCertificate(
                    new ByteArrayInputStream(corpoPem(credenciais.certificadoPem(), "CERTIFICATE")));

            PrivateKey chavePrivada = KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(chavePrivadaPkcs8(credenciais.chavePrivadaPem())));

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("cora-mtls", chavePrivada, new char[0], new Certificate[]{certificado});

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, new char[0]);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
            return sslContext;
        } catch (GeneralSecurityException | IllegalArgumentException | java.io.IOException e) {
            throw new IllegalStateException("falha ao montar mTLS para o Cora", e);
        }
    }

    private byte[] corpoPem(String pem, String tipo) {
        String limpo = pem
                .replace("-----BEGIN " + tipo + "-----", "")
                .replace("-----END " + tipo + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(limpo);
    }

    private byte[] chavePrivadaPkcs8(String pem) {
        if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            return encapsularPkcs1EmPkcs8(corpoPem(pem, "RSA PRIVATE KEY"));
        }
        return corpoPem(pem, "PRIVATE KEY");
    }

    private byte[] encapsularPkcs1EmPkcs8(byte[] pkcs1) {
        byte[] rsaOid = new byte[]{
                0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
                (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00};
        byte[] version = new byte[]{0x02, 0x01, 0x00};
        byte[] privateKey = derOctetString(pkcs1);
        return derSequence(concat(version, rsaOid, privateKey));
    }

    private byte[] derSequence(byte[] conteudo) {
        return concat(new byte[]{0x30}, derLength(conteudo.length), conteudo);
    }

    private byte[] derOctetString(byte[] conteudo) {
        return concat(new byte[]{0x04}, derLength(conteudo.length), conteudo);
    }

    private byte[] derLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int valor = length;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        while (valor > 0) {
            bytes.write(valor & 0xff);
            valor >>= 8;
        }
        byte[] invertido = bytes.toByteArray();
        byte[] resultado = new byte[invertido.length + 1];
        resultado[0] = (byte) (0x80 | invertido.length);
        for (int i = 0; i < invertido.length; i++) {
            resultado[i + 1] = invertido[invertido.length - 1 - i];
        }
        return resultado;
    }

    private byte[] concat(byte[]... partes) {
        int total = 0;
        for (byte[] parte : partes) {
            total += parte.length;
        }
        byte[] resultado = new byte[total];
        int pos = 0;
        for (byte[] parte : partes) {
            System.arraycopy(parte, 0, resultado, pos, parte.length);
            pos += parte.length;
        }
        return resultado;
    }
}
