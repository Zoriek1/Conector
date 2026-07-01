package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstadoOAuthBlingTest {

    private static final Instant AGORA = Instant.parse("2026-06-30T12:00:00Z");
    private static final String CHAVE = Base64.getEncoder()
            .encodeToString(new byte[32]); // chave AES-256 de teste

    @Test
    void geraEValidaPreservandoEmpresaENonce() {
        EstadoOAuthBling estado = comRelogio(AGORA);
        UUID empresaId = UUID.randomUUID();

        EstadoOAuthBling.Emitido emitido = estado.gerar(empresaId);
        EstadoOAuthBling.Conteudo conteudo = estado.validar(emitido.valor());

        assertThat(conteudo.empresaId()).isEqualTo(empresaId);
        assertThat(conteudo.nonce()).isEqualTo(emitido.nonce());
    }

    @Test
    void rejeitaAssinaturaAdulterada() {
        EstadoOAuthBling estado = comRelogio(AGORA);
        String valor = estado.gerar(UUID.randomUUID()).valor();
        String adulterado = valor.substring(0, valor.indexOf('.')) + ".XXXX";

        assertThatThrownBy(() -> estado.validar(adulterado))
                .isInstanceOf(FalhaBlingException.class);
    }

    @Test
    void rejeitaEstadoExpirado() {
        UUID empresaId = UUID.randomUUID();
        String valor = comRelogio(AGORA).gerar(empresaId).valor();

        EstadoOAuthBling depois = comRelogio(AGORA.plus(Duration.ofMinutes(11)));
        assertThatThrownBy(() -> depois.validar(valor))
                .isInstanceOf(FalhaBlingException.class);
    }

    @Test
    void rejeitaStateAusente() {
        assertThatThrownBy(() -> comRelogio(AGORA).validar(null))
                .isInstanceOf(FalhaBlingException.class);
    }

    private static EstadoOAuthBling comRelogio(Instant instante) {
        ConciliadorProperties properties = mock(ConciliadorProperties.class);
        when(properties.cripto()).thenReturn(new ConciliadorProperties.Cripto(CHAVE));
        return new EstadoOAuthBling(properties, Clock.fixed(instante, ZoneOffset.UTC));
    }
}
