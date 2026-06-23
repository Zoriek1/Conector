package com.planteumaflor.conciliador.pluggy.domain;

/**
 * Estado de saúde da conexão Pluggy de uma empresa (tela 07 §7.2).
 *
 * No fake (passo 3) usamos sobretudo NAO_CONECTADA e ATIVA. Os demais existem
 * para o adapter real lidar com reconexão e consentimento expirado.
 */
public enum StatusIntegracao {
    NAO_CONECTADA,
    ATIVA,
    REQUER_ATENCAO,
    DESCONECTADA
}
