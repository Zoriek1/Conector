package com.planteumaflor.conciliador.cora.domain;

/** Estado de saúde da integração direta com a API do Cora. */
public enum StatusIntegracaoCora {
    NAO_CONECTADA,
    ATIVA,
    REQUER_ATENCAO,
    DESCONECTADA
}
