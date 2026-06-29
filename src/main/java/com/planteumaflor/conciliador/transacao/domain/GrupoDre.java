package com.planteumaflor.conciliador.transacao.domain;

/** Grupo gerencial usado para separar categorias que entram ou nao no DRE. */
public enum GrupoDre {
    RECEITA_OPERACIONAL("Receita operacional"),
    DEDUCOES_RECEITA("Deduções da receita"),
    CMV("Custo da mercadoria vendida"),
    DESPESAS_OPERACIONAIS("Despesas operacionais"),
    DESPESAS_COMERCIAIS("Despesas comerciais"),
    DESPESAS_FINANCEIRAS("Despesas financeiras"),
    OUTRAS_RECEITAS("Outras receitas"),
    OUTRAS_DESPESAS("Outras despesas"),
    NAO_DRE("Fora do DRE"),
    INDEFINIDO("Indefinido");

    private final String rotulo;

    GrupoDre(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
