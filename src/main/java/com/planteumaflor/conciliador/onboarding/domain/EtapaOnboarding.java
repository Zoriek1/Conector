package com.planteumaflor.conciliador.onboarding.domain;

/**
 * Etapa do onboarding, DERIVADA do estado persistido (tela 03 §2) — nunca
 * controlada por um número de etapa enviado pelo navegador.
 *
 * O v1 operacional aceita Cora ou Pluggy como conector ativo.
 */
public enum EtapaOnboarding {
    INTEGRACOES_PENDENTES,
    CONTAS_PENDENTES,
    CONCLUIDO
}
