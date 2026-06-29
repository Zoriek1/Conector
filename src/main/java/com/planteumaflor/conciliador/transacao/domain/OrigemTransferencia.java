package com.planteumaflor.conciliador.transacao.domain;

/** Como o pareamento de uma transferência interna foi criado. */
public enum OrigemTransferencia {
    /** Detectado automaticamente pelo casamento de valor e data entre contas. */
    AUTOMATICA,
    /** Pareado manualmente por um humano na revisão. */
    MANUAL
}
