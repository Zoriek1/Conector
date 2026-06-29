package com.planteumaflor.conciliador.transacao.domain;

import java.util.Arrays;
import java.util.List;

/** Natureza financeira atribuida durante a classificacao. */
public enum ClasseTransacao {
    CREDITO_VENDA("Venda recebida", Direcao.CREDITO, GrupoDre.RECEITA_OPERACIONAL, true, false),
    RECEITA_FINANCEIRA("Receita financeira", Direcao.CREDITO, GrupoDre.OUTRAS_RECEITAS, true, false),
    ESTORNO_REEMBOLSO_RECEBIDO(
            "Estorno ou reembolso recebido",
            Direcao.CREDITO,
            GrupoDre.OUTRAS_RECEITAS,
            false,
            true),
    APORTE_SOCIO("Aporte de socio", Direcao.CREDITO, GrupoDre.NAO_DRE, false, true),
    EMPRESTIMO_RECEBIDO("Emprestimo recebido", Direcao.CREDITO, GrupoDre.NAO_DRE, false, true),
    APORTE_EMPRESTIMO(
            "Aporte ou emprestimo (legado)",
            Direcao.CREDITO,
            GrupoDre.NAO_DRE,
            false,
            true,
            false),

    TRANSFERENCIA_INTERNA("Transferencia interna", null, GrupoDre.NAO_DRE, false, true),

    FLORES_FOLHAGENS_PLANTAS("Flores, folhagens e plantas", Direcao.DEBITO, GrupoDre.CMV, true, true),
    EMBALAGENS_COMPLEMENTOS("Embalagens e complementos", Direcao.DEBITO, GrupoDre.CMV, true, true),
    INSUMOS_JARDINAGEM("Insumos para jardinagem", Direcao.DEBITO, GrupoDre.CMV, true, true),
    FRETE_ENTREGAS("Frete e entregas", Direcao.DEBITO, GrupoDre.CMV, true, true),
    OUTROS_CUSTOS_DIRETOS("Outros custos diretos", Direcao.DEBITO, GrupoDre.CMV, true, true),
    FORNECEDORES_CMV(
            "Fornecedores e CMV (legado)",
            Direcao.DEBITO,
            GrupoDre.CMV,
            true,
            true,
            false),

    MARKETING_TRAFEGO("Marketing e trafego pago", Direcao.DEBITO, GrupoDre.DESPESAS_COMERCIAIS, true, true),
    MARKETING_VENDAS(
            "Marketing e vendas (legado)",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_COMERCIAIS,
            true,
            true,
            false),
    TAXAS_VENDAS("Taxas de venda", Direcao.DEBITO, GrupoDre.DEDUCOES_RECEITA, true, true),
    VENDA_CANCELADA_ESTORNO(
            "Venda cancelada ou estorno",
            Direcao.DEBITO,
            GrupoDre.DEDUCOES_RECEITA,
            true,
            true),
    COMISSOES_VENDA("Comissoes de venda", Direcao.DEBITO, GrupoDre.DESPESAS_COMERCIAIS, true, true),
    FERRAMENTAS_VENDA_ATENDIMENTO(
            "Ferramentas de venda e atendimento",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_COMERCIAIS,
            true,
            true),

    SALARIOS_ENCARGOS("Salarios e encargos", Direcao.DEBITO, GrupoDre.DESPESAS_OPERACIONAIS, true, true),
    PRO_LABORE("Pro-labore", Direcao.DEBITO, GrupoDre.DESPESAS_OPERACIONAIS, true, true),
    RETIRADA_DISTRIBUICAO_SOCIOS(
            "Retirada ou distribuicao de socios",
            Direcao.DEBITO,
            GrupoDre.NAO_DRE,
            false,
            true),

    ALUGUEL_INFRA("Aluguel e infraestrutura", Direcao.DEBITO, GrupoDre.DESPESAS_OPERACIONAIS, true, true),
    ALIMENTACAO_EQUIPE("Alimentacao", Direcao.DEBITO, GrupoDre.DESPESAS_OPERACIONAIS, true, true),
    COMBUSTIVEL_VEICULO(
            "Gasolina e combustivel",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true),
    MANUTENCAO_VEICULO(
            "Manutencao de veiculo",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true),
    PEDAGIO_ESTACIONAMENTO(
            "Pedagio e estacionamento",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true),
    AGUA_ENERGIA_INTERNET_TELEFONE(
            "Agua, energia, internet e telefone",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true),
    SISTEMAS_ASSINATURAS("Sistemas e assinaturas", Direcao.DEBITO, GrupoDre.DESPESAS_OPERACIONAIS, true, true),
    MANUTENCAO_LIMPEZA_MATERIAIS(
            "Manutencao, limpeza e materiais internos",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true),

    CONTABILIDADE_SERVICOS(
            "Contabilidade e servicos profissionais",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true),
    IMPOSTOS_TRIBUTOS("Impostos e tributos", Direcao.DEBITO, GrupoDre.DESPESAS_OPERACIONAIS, true, true),
    TARIFAS_BANCARIAS("Tarifas bancarias", Direcao.DEBITO, GrupoDre.DESPESAS_FINANCEIRAS, true, true),
    JUROS_MULTAS("Juros e multas", Direcao.DEBITO, GrupoDre.DESPESAS_FINANCEIRAS, true, true),
    TARIFAS_JUROS(
            "Tarifas e juros (legado)",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_FINANCEIRAS,
            true,
            true,
            false),

    PAGAMENTO_EMPRESTIMO("Pagamento de emprestimo", Direcao.DEBITO, GrupoDre.NAO_DRE, false, true),
    INVESTIMENTOS_EQUIPAMENTOS(
            "Investimentos, maquinas e equipamentos",
            Direcao.DEBITO,
            GrupoDre.NAO_DRE,
            false,
            true),
    DEBITO_DESPESA(
            "Despesa operacional (legado)",
            Direcao.DEBITO,
            GrupoDre.DESPESAS_OPERACIONAIS,
            true,
            true,
            false),
    FRETE_LOGISTICA(
            "Frete e logistica (legado)",
            Direcao.DEBITO,
            GrupoDre.CMV,
            true,
            true,
            false),

    OUTRA_ENTRADA("Outra entrada", Direcao.CREDITO, GrupoDre.OUTRAS_RECEITAS, false, true),
    OUTRA_SAIDA("Outra saida", Direcao.DEBITO, GrupoDre.OUTRAS_DESPESAS, false, true),
    INDEFINIDO("Indefinido", null, GrupoDre.INDEFINIDO, false, false, false);

    private final String rotulo;
    private final Direcao direcaoPermitida;
    private final GrupoDre grupoDre;
    private final boolean podeApi;
    private final boolean podeOfx;
    private final boolean ativo;

    ClasseTransacao(
            String rotulo,
            Direcao direcaoPermitida,
            GrupoDre grupoDre,
            boolean podeApi,
            boolean podeOfx) {
        this(rotulo, direcaoPermitida, grupoDre, podeApi, podeOfx, true);
    }

    ClasseTransacao(
            String rotulo,
            Direcao direcaoPermitida,
            GrupoDre grupoDre,
            boolean podeApi,
            boolean podeOfx,
            boolean ativo) {
        this.rotulo = rotulo;
        this.direcaoPermitida = direcaoPermitida;
        this.grupoDre = grupoDre;
        this.podeApi = podeApi;
        this.podeOfx = podeOfx;
        this.ativo = ativo;
    }

    public boolean aceita(Direcao direcao) {
        return direcaoPermitida == null || direcaoPermitida == direcao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public Direcao getDirecaoPermitida() {
        return direcaoPermitida;
    }

    public GrupoDre getGrupoDre() {
        return grupoDre;
    }

    public boolean isPodeApi() {
        return podeApi;
    }

    public boolean isPodeOfx() {
        return podeOfx;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public static List<ClasseTransacao> classificaveis() {
        return Arrays.stream(values())
                .filter(classe -> classe.ativo)
                .filter(classe -> classe != INDEFINIDO)
                .toList();
    }

    public static List<ClasseTransacao> classificaveisPara(Direcao direcao) {
        return classificaveis().stream()
                .filter(classe -> classe.aceita(direcao))
                .toList();
    }
}
