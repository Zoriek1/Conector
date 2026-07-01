package com.planteumaflor.conciliador.pluggy.webhook;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autentica chamadas de webhook da Pluggy.
 *
 * A Pluggy não assina o corpo (sem HMAC): a autenticidade vem de um header
 * customizado definido no momento do registro do webhook ({@code POST /webhooks}),
 * que a Pluggy ecoa em toda chamada. Aqui só comparamos esse valor com o
 * segredo configurado — em tempo constante, para não vazar o segredo por
 * timing attack.
 */
@Component
class PluggyWebhookAuthenticator {

    private final String segredo;

    PluggyWebhookAuthenticator(ConciliadorProperties properties) {
        this.segredo = properties.pluggy().webhookSecret();
    }

    boolean valido(String headerRecebido) {
        if (segredo == null || segredo.isBlank() || headerRecebido == null || headerRecebido.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                segredo.getBytes(StandardCharsets.UTF_8),
                headerRecebido.getBytes(StandardCharsets.UTF_8));
    }
}
