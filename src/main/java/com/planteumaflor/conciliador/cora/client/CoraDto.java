package com.planteumaflor.conciliador.cora.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs do contrato JSON externo do Cora — não escapam do pacote {@code client}.
 *
 * ATENÇÃO: os nomes de campo de {@link SaldoResponse}/{@link ExtratoResponse}/
 * {@link Lancamento} são PROVISÓRIOS. Não foi possível validar o schema real
 * contra os docs/sandbox do Cora (developers.cora.com.br bloqueou fetch
 * automatizado); confirmar e ajustar antes de produção (risco documentado no
 * plano de integração). {@link TokenResponse} segue o vocabulário padrão do
 * RFC 6749 (client-credentials), que é estável independente do provedor.
 */
final class CoraDto {

    private CoraDto() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SaldoResponse(
            @JsonProperty("saldoDisponivel")
            @JsonAlias({"available", "availableBalance", "balance", "amount"})
            BigDecimal saldoDisponivel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExtratoResponse(
            @JsonProperty("lancamentos")
            @JsonAlias({"results", "items", "data", "transactions"})
            List<Lancamento> lancamentos,
            @JsonProperty("hasMore") Boolean hasMore,
            @JsonProperty("next") String next) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Lancamento(
            @JsonProperty("id")
            @JsonAlias({"transactionId", "code"})
            String id,
            @JsonProperty("contaId")
            @JsonAlias({"accountId", "account_id"})
            String contaId,
            @JsonProperty("dataLancamento")
            @JsonAlias({"date", "created_at", "createdAt", "transactionDate"})
            LocalDate dataLancamento,
            @JsonProperty("valor")
            @JsonAlias({"amount", "value"})
            BigDecimal valor,
            @JsonProperty("descricao")
            @JsonAlias({"description", "memo"})
            String descricao,
            @JsonProperty("documentoContraparte")
            @JsonAlias({"counterpartyDocument", "document", "taxId"})
            String documentoContraparte,
            @JsonProperty("endToEndId")
            @JsonAlias({"e2eId", "end_to_end_id"})
            String endToEndId) {
    }
}
