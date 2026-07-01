package com.planteumaflor.conciliador.bling.application;

import java.time.Instant;

/**
 * Porta para o endpoint OAuth do Bling. A camada {@code application} conhece
 * apenas este contrato; a implementação HTTP vive em {@code client}.
 */
public interface BlingOAuthGateway {

    /** Troca o {@code code} do callback por um par de tokens. */
    TokensBling trocarCodigo(String code, String redirectUri);

    /** Renova o access token a partir do refresh token. */
    TokensBling renovar(String refreshToken);

    /** Tokens em claro recém-obtidos do Bling, com a expiração já calculada. */
    record TokensBling(String accessToken, String refreshToken, Instant expiraEm) {
    }
}
