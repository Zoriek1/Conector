package com.planteumaflor.conciliador.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriptoServiceTest {

    @Test
    void cifraEDecifraDeVolvendoOTextoOriginal() {
        CriptoService cripto = new CriptoService(propriedadesCom(chaveAleatoria()));

        String cifrado = cripto.cifrar("certificado-secreto-da-empresa");

        assertThat(cifrado).isNotEqualTo("certificado-secreto-da-empresa");
        assertThat(cripto.decifrar(cifrado)).isEqualTo("certificado-secreto-da-empresa");
    }

    @Test
    void cadaChamadaDeCifrarGeraSaidaDiferente() {
        CriptoService cripto = new CriptoService(propriedadesCom(chaveAleatoria()));

        String cifrado1 = cripto.cifrar("mesmo-texto");
        String cifrado2 = cripto.cifrar("mesmo-texto");

        assertThat(cifrado1).isNotEqualTo(cifrado2);
    }

    @Test
    void rejeitaQuandoChaveNaoConfigurada() {
        CriptoService cripto = new CriptoService(propriedadesCom(""));

        assertThatThrownBy(() -> cripto.cifrar("texto"))
                .isInstanceOf(IllegalStateException.class);
    }

    private String chaveAleatoria() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private ConciliadorProperties propriedadesCom(String chave) {
        return new ConciliadorProperties(
                "America/Sao_Paulo",
                new ConciliadorProperties.Ingest("0 0 4 * * *", 7),
                new ConciliadorProperties.Classificacao(
                        new BigDecimal("0.900"), new BigDecimal("0.100")),
                new ConciliadorProperties.Bling(null, null, null, Duration.ofMinutes(2)),
                new ConciliadorProperties.Pluggy(null),
                new ConciliadorProperties.Cora(null, null, null, null),
                new ConciliadorProperties.Cripto(chave));
    }
}
