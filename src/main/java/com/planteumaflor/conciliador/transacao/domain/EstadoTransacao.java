package com.planteumaflor.conciliador.transacao.domain;

/** Estado persistido do movimento ao longo do pipeline de conciliação. */
public enum EstadoTransacao {
    INGERIDO,
    CLASSIFICADO,
    EM_REVISAO,
    AGUARDANDO_ESCRITA_API,
    ESCRITO_API,
    EM_LOTE_OFX,
    CONCILIADO,
    FALHA
}
