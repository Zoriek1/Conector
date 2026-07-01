package com.planteumaflor.conciliador.bling.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payloads do endpoint OAuth do Bling (Bling-API-v3 §6). Apenas o necessário
 * para o fluxo authorization_code + refresh_token; demais campos são ignorados.
 */
final class BlingDtos {

    private BlingDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("scope") String scope) {
    }
}
