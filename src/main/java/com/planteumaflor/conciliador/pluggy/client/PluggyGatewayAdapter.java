package com.planteumaflor.conciliador.pluggy.client;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.pluggy.application.PluggyGateway;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
class PluggyGatewayAdapter implements PluggyGateway {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final RestClient client;

    PluggyGatewayAdapter(ConciliadorProperties properties) {
        this.client = RestClient.builder()
                .baseUrl(properties.pluggy().baseUrl())
                .build();
    }

    @Override
    public String criarApiKey(CredenciaisPluggy credenciais) {
        PluggyDto.AuthResponse resposta = client.post()
                .uri("/auth")
                .body(new PluggyDto.AuthRequest(credenciais.clientId(), credenciais.clientSecret()))
                .retrieve()
                .body(PluggyDto.AuthResponse.class);
        if (resposta == null || resposta.apiKey() == null || resposta.apiKey().isBlank()) {
            throw new IllegalStateException("Pluggy não retornou apiKey");
        }
        return resposta.apiKey();
    }

    @Override
    public ConnectToken criarConnectToken(String apiKey, String clientUserId) {
        PluggyDto.ConnectTokenResponse resposta = client.post()
                .uri("/connect_token")
                .header(API_KEY_HEADER, apiKey)
                .body(new PluggyDto.ConnectTokenRequest(
                        new PluggyDto.ConnectTokenOptions(clientUserId, true)))
                .retrieve()
                .body(PluggyDto.ConnectTokenResponse.class);
        if (resposta == null || resposta.accessToken() == null || resposta.accessToken().isBlank()) {
            throw new IllegalStateException("Pluggy não retornou connectToken");
        }
        return new ConnectToken(resposta.accessToken());
    }

    @Override
    public List<ContaPluggy> listarContas(String apiKey, String itemId) {
        PluggyDto.AccountsResponse resposta = client.get()
                .uri(uriBuilder -> uriBuilder.path("/accounts")
                        .queryParam("itemId", itemId)
                        .build())
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .body(PluggyDto.AccountsResponse.class);
        List<ContaPluggy> contas = new ArrayList<>();
        if (resposta != null && resposta.results() != null) {
            for (PluggyDto.Account conta : resposta.results()) {
                if (conta.id() != null) {
                    contas.add(new ContaPluggy(
                            conta.id(),
                            conta.itemId(),
                            primeiroTexto(conta.marketingName(), conta.name(), "Conta Pluggy"),
                            conta.number(),
                            primeiroTexto(conta.subtype(), conta.type(), "OUTRA"),
                            conta.bankData() == null ? null : conta.bankData().routingNumber()));
                }
            }
        }
        return contas;
    }

    @Override
    public List<TransacaoPluggy> listarTransacoes(String apiKey, String accountId, LocalDate de, LocalDate ate) {
        List<TransacaoPluggy> transacoes = new ArrayList<>();
        String after = null;
        for (int pagina = 0; pagina < 100; pagina++) {
            String cursor = after;
            PluggyDto.TransactionsResponse resposta = client.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v2/transactions")
                                .queryParam("accountId", accountId)
                                .queryParam("dateFrom", de)
                                .queryParam("dateTo", ate);
                        if (cursor != null) {
                            builder.queryParam("after", cursor);
                        }
                        return builder.build();
                    })
                    .header(API_KEY_HEADER, apiKey)
                    .retrieve()
                    .body(PluggyDto.TransactionsResponse.class);

            if (resposta == null || resposta.results() == null || resposta.results().isEmpty()) {
                break;
            }
            for (PluggyDto.Transaction tx : resposta.results()) {
                if (tx.id() != null && tx.date() != null && tx.amount() != null) {
                    transacoes.add(new TransacaoPluggy(
                            tx.id(),
                            tx.accountId(),
                            tx.date().toLocalDate(),
                            tx.amount(),
                            tx.type(),
                            primeiroTexto(tx.descriptionRaw(), tx.description(), "Transação Pluggy"),
                            tx.descriptionRaw(),
                            tx.providerCode(),
                            documentoContraparte(tx.paymentData())));
                }
            }
            after = extrairAfter(resposta.next());
            if (after == null) {
                break;
            }
        }
        return transacoes;
    }

    @Override
    public void registrarWebhook(String apiKey, String url, String evento, Map<String, String> headers) {
        client.post()
                .uri("/webhooks")
                .header(API_KEY_HEADER, apiKey)
                .body(new PluggyDto.WebhookRequest(url, evento, headers))
                .retrieve()
                .toBodilessEntity();
    }

    private String documentoContraparte(PluggyDto.PaymentData paymentData) {
        if (paymentData == null) {
            return null;
        }
        if (paymentData.payer() != null && paymentData.payer().documentNumber() != null) {
            return paymentData.payer().documentNumber().value();
        }
        if (paymentData.receiver() != null && paymentData.receiver().documentNumber() != null) {
            return paymentData.receiver().documentNumber().value();
        }
        return null;
    }

    private String extrairAfter(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        for (String parte : next.replace("?", "").split("&")) {
            int idx = parte.indexOf('=');
            if (idx > 0 && parte.substring(0, idx).equals("after")) {
                return URLDecoder.decode(parte.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String primeiroTexto(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return "";
    }
}
