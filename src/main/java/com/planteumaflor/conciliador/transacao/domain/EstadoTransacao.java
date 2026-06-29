package com.planteumaflor.conciliador.transacao.domain;

/** Estado persistido do movimento ao longo do pipeline de conciliação. */
public enum EstadoTransacao {
    INGERIDO("Ingerido"),
    CLASSIFICADO("Classificado"),
    EM_REVISAO("Em revisão"),
    AGUARDANDO_ESCRITA_API("Aguardando envio"),
    ESCRITO_API("Enviado"),
    EM_LOTE_OFX("Em lote OFX"),
    CONCILIADO("Conciliado"),
    FALHA("Falha");

    private final String rotulo;

    EstadoTransacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
