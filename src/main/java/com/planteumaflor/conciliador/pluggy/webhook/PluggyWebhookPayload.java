package com.planteumaflor.conciliador.pluggy.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo de um webhook Pluggy (docs.pluggy.ai/docs/webhooks).
 *
 * Só os campos comuns a todos os eventos são mapeados aqui — {@code event} e
 * {@code itemId} bastam para o roteamento, que sempre revalida o estado via
 * {@code GET}/pull na Pluggy em vez de confiar nos demais campos do payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record PluggyWebhookPayload(String event, String eventId, String itemId) {
}
