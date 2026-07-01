package com.planteumaflor.conciliador.pluggy.webhook;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluggyWebhookAuthenticatorTest {

    @Test
    void aceitaHeaderIgualAoSegredoConfigurado() {
        PluggyWebhookAuthenticator authenticator = comSegredo("segredo-123");

        assertThat(authenticator.valido("segredo-123")).isTrue();
    }

    @Test
    void rejeitaHeaderDiferente() {
        PluggyWebhookAuthenticator authenticator = comSegredo("segredo-123");

        assertThat(authenticator.valido("outro-valor")).isFalse();
    }

    @Test
    void rejeitaHeaderAusente() {
        PluggyWebhookAuthenticator authenticator = comSegredo("segredo-123");

        assertThat(authenticator.valido(null)).isFalse();
    }

    @Test
    void rejeitaQuandoSegredoNaoEstaConfigurado() {
        PluggyWebhookAuthenticator authenticator = comSegredo("");

        assertThat(authenticator.valido("qualquer-coisa")).isFalse();
    }

    private PluggyWebhookAuthenticator comSegredo(String segredo) {
        ConciliadorProperties properties = mock(ConciliadorProperties.class);
        when(properties.pluggy()).thenReturn(new ConciliadorProperties.Pluggy(
                "https://api.pluggy.ai", segredo, "http://localhost:8080/page"));
        return new PluggyWebhookAuthenticator(properties);
    }
}
