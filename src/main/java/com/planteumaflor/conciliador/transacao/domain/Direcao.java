package com.planteumaflor.conciliador.transacao.domain;

/** Sinal semantico do movimento; o valor monetario permanece sempre positivo. */
public enum Direcao {
    CREDITO("Entrada"),
    DEBITO("Saida");

    private final String rotulo;

    Direcao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
