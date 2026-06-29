package com.planteumaflor.conciliador.conta.domain;

public enum TipoContaBancaria {
    CORRENTE("Conta corrente"),
    POUPANCA("Poupança"),
    PAGAMENTO("Conta pagamento"),
    CARTAO_CREDITO("Cartão de crédito"),
    OUTRA("Outra");

    private final String rotulo;

    TipoContaBancaria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
