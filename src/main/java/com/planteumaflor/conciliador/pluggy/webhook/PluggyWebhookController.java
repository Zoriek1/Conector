package com.planteumaflor.conciliador.pluggy.webhook;

import com.planteumaflor.conciliador.pluggy.application.PluggyIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe notificações de webhook da Pluggy (item/transactions).
 *
 * Chamada servidor-a-servidor, sem sessão/CSRF (autenticidade vem do header
 * {@code X-Webhook-Secret}, ver {@link PluggyWebhookAuthenticator} — a Pluggy
 * não assina o corpo). A Pluggy exige 2XX em até 5s e manda processar DEPOIS
 * de responder, por isso o processamento é despachado de forma assíncrona
 * ({@link PluggyIntegrationService#processarEventoItem} e
 * {@link PluggyIntegrationService#processarEventoTransacoes} são {@code @Async}).
 */
@RestController
@RequestMapping("/webhooks/pluggy")
class PluggyWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PluggyWebhookController.class);

    private final PluggyIntegrationService pluggy;
    private final PluggyWebhookAuthenticator authenticator;

    PluggyWebhookController(PluggyIntegrationService pluggy, PluggyWebhookAuthenticator authenticator) {
        this.pluggy = pluggy;
        this.authenticator = authenticator;
    }

    @PostMapping
    ResponseEntity<Void> receber(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String segredo,
            @RequestBody PluggyWebhookPayload evento) {
        if (!authenticator.valido(segredo)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        switch (evento.event()) {
            case "item/created", "item/updated" -> pluggy.processarEventoItem(evento.itemId());
            case "transactions/created", "transactions/updated" -> pluggy.processarEventoTransacoes(evento.itemId());
            case "transactions/deleted" -> log.info(
                    "webhook Pluggy: transactions/deleted recebido para item {} — sem suporte a remoção hoje, ignorando",
                    evento.itemId());
            case null, default -> log.info("webhook Pluggy: evento {} ignorado", evento.event());
        }
        return ResponseEntity.ok().build();
    }
}
