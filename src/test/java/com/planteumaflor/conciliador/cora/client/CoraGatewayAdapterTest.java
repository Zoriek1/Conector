package com.planteumaflor.conciliador.cora.client;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.cora.application.CoraGateway.CredenciaisCora;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica só a plumbing de mTLS (montagem do SSLContext em memória a partir
 * de um certificado/chave PEM autoassinados de teste) — não o schema JSON do
 * Cora, que é o risco documentado e não verificável sem o sandbox real.
 */
class CoraGatewayAdapterTest {

    private final CoraGatewayAdapter adapter = new CoraGatewayAdapter(propriedades());

    @Test
    void montaSslContextAPartirDeCertificadoEChavePemValidos() throws Exception {
        CredenciaisCora credenciais = new CredenciaisCora(
                "client-de-teste", lerRecurso("cora/test-cert.pem"), lerRecurso("cora/test-key.pem"));

        SSLContext sslContext = adapter.montarSslContext(credenciais);

        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getSocketFactory()).isNotNull();
    }

    @Test
    void rejeitaCertificadoInvalido() {
        CredenciaisCora credenciais = new CredenciaisCora(
                "client-de-teste", "não é um certificado", lerRecurso("cora/test-key.pem"));

        assertThatThrownBy(() -> adapter.montarSslContext(credenciais))
                .isInstanceOf(IllegalStateException.class);
    }

    private String lerRecurso(String caminho) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (in == null) {
                throw new IllegalStateException("recurso de teste não encontrado: " + caminho);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConciliadorProperties propriedades() {
        return new ConciliadorProperties(
                "America/Sao_Paulo",
                new ConciliadorProperties.Ingest("0 0 4 * * *", 7),
                new ConciliadorProperties.Classificacao(
                        new java.math.BigDecimal("0.900"), new java.math.BigDecimal("0.100")),
                new ConciliadorProperties.Bling(null, null, null, java.time.Duration.ofMinutes(2)),
                new ConciliadorProperties.Pluggy(null),
                new ConciliadorProperties.Cora(
                        "https://example.invalid", "/token", "/extrato", "/saldo"),
                new ConciliadorProperties.Cripto(null));
    }
}
