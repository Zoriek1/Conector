package com.planteumaflor.conciliador.cora.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
            @JsonProperty("saldoDisponivel") BigDecimal saldoDisponivel) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExtratoResponse(
            @JsonProperty("lancamentos") List<Lancamento> lancamentos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Lancamento(
            @JsonProperty("id") String id,
            @JsonProperty("contaId") String contaId,
            @JsonProperty("dataLancamento") LocalDate dataLancamento,
            @JsonProperty("valor") BigDecimal valor,
            @JsonProperty("descricao") String descricao,
            @JsonProperty("documentoContraparte") String documentoContraparte,
            @JsonProperty("endToEndId") String endToEndId) {
    }
}
