package com.planteumaflor.conciliador.cora.client;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.cora.application.CoraGateway;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
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

    private final String baseUrl;
    private final String tokenPath;
    private final String extratoPath;
    private final String saldoPath;

    CoraGatewayAdapter(ConciliadorProperties properties) {
        this.baseUrl = properties.cora().baseUrl();
        this.tokenPath = properties.cora().tokenPath();
        this.extratoPath = properties.cora().extratoPath();
        this.saldoPath = properties.cora().saldoPath();
    }

    @Override
    public BigDecimal saldo(CredenciaisCora credenciais) {
        RestClient client = restClient(credenciais);
        String token = autenticar(client, credenciais);
        CoraDto.SaldoResponse resposta = client.get()
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
        RestClient client = restClient(credenciais);
        String token = autenticar(client, credenciais);
        CoraDto.ExtratoResponse resposta = client.get()
                .uri(uriBuilder -> uriBuilder.path(extratoPath)
                        .queryParam("dataInicio", de)
                        .queryParam("dataFim", ate)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(CoraDto.ExtratoResponse.class);

        List<LancamentoExtrato> lancamentos = new ArrayList<>();
        if (resposta != null && resposta.lancamentos() != null) {
            for (CoraDto.Lancamento lancamento : resposta.lancamentos()) {
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
        return lancamentos;
    }

    private String autenticar(RestClient client, CredenciaisCora credenciais) {
        CoraDto.TokenResponse resposta = client.post()
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

    private RestClient restClient(CredenciaisCora credenciais) {
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
                    new PKCS8EncodedKeySpec(corpoPem(credenciais.chavePrivadaPem(), "PRIVATE KEY")));

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
}
