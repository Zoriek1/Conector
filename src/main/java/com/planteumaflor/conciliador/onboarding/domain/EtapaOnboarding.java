package com.planteumaflor.conciliador.onboarding.domain;

/**
 * Etapa do onboarding, DERIVADA do estado persistido (tela 03 §2) — nunca
 * controlada por um número de etapa enviado pelo navegador.
 *
 * No passo 3 (fake) modelamos o essencial: falta conectar o Pluggy, ou já está
 * concluído. As etapas intermediárias (Bling, primeira sincronização) entram
 * quando essas integrações forem implementadas.
 */
public enum EtapaOnboarding {
    PLUGGY_PENDENTE,
    CONCLUIDO
}
