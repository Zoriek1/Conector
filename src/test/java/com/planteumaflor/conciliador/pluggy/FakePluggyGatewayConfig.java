package com.planteumaflor.conciliador.pluggy;

import com.planteumaflor.conciliador.pluggy.application.PluggyGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Substitui o {@code PluggyGatewayAdapter} real (HTTP) por um fake controlável
 * nos testes de integração, sem rede.
 */
@TestConfiguration
public class FakePluggyGatewayConfig {

    @Bean
    @Primary
    public FakePluggyGateway pluggyGateway() {
        return new FakePluggyGateway();
    }

    public static final class FakePluggyGateway implements PluggyGateway {

        public record WebhookRegistrado(String apiKey, String url, String evento, Map<String, String> headers) {}

        private List<ContaPluggy> contas = List.of();
        private List<TransacaoPluggy> transacoes = List.of();
        private final List<WebhookRegistrado> webhooksRegistrados = new CopyOnWriteArrayList<>();
        private RuntimeException falhaRegistrarWebhook;

        public void definirContas(List<ContaPluggy> contas) {
            this.contas = contas;
        }

        public void definirTransacoes(List<TransacaoPluggy> transacoes) {
            this.transacoes = transacoes;
        }

        public void definirFalhaRegistrarWebhook(RuntimeException falha) {
            this.falhaRegistrarWebhook = falha;
        }

        public List<WebhookRegistrado> webhooksRegistrados() {
            return new ArrayList<>(webhooksRegistrados);
        }

        @Override
        public String criarApiKey(CredenciaisPluggy credenciais) {
            return "fake-api-key-" + credenciais.clientId();
        }

        @Override
        public ConnectToken criarConnectToken(String apiKey, String clientUserId) {
            return new ConnectToken("fake-connect-token");
        }

        @Override
        public List<ContaPluggy> listarContas(String apiKey, String itemId) {
            return contas;
        }

        @Override
        public List<TransacaoPluggy> listarTransacoes(String apiKey, String accountId, LocalDate de, LocalDate ate) {
            return transacoes;
        }

        @Override
        public void registrarWebhook(String apiKey, String url, String evento, Map<String, String> headers) {
            if (falhaRegistrarWebhook != null) {
                throw falhaRegistrarWebhook;
            }
            webhooksRegistrados.add(new WebhookRegistrado(apiKey, url, evento, headers));
        }
    }
}
