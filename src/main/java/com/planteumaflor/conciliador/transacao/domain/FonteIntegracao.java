package com.planteumaflor.conciliador.transacao.domain;

/** Origem de onde a transação foi ingerida. */
public enum FonteIntegracao {
    PLUGGY("Pluggy"),
    CORA("Cora");

    private final String rotulo;

    FonteIntegracao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
