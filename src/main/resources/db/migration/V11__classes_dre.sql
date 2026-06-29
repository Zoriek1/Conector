-- Expande as classes fixas para categorias operacionais com grupo de DRE.

ALTER TABLE transacao
    DROP CONSTRAINT ck_transacao_classe;

ALTER TABLE transacao
    ADD CONSTRAINT ck_transacao_classe
        CHECK (classe IN (
            'CREDITO_VENDA',
            'TRANSFERENCIA_INTERNA',
            'DEBITO_DESPESA',
            'PRO_LABORE',
            'RECEITA_FINANCEIRA',
            'ESTORNO_REEMBOLSO_RECEBIDO',
            'APORTE_EMPRESTIMO',
            'FORNECEDORES_CMV',
            'TAXAS_VENDAS',
            'TARIFAS_JUROS',
            'IMPOSTOS_TRIBUTOS',
            'MARKETING_VENDAS',
            'FRETE_LOGISTICA',
            'ALUGUEL_INFRA',
            'SISTEMAS_ASSINATURAS',
            'CONTABILIDADE_SERVICOS',
            'RETIRADA_DISTRIBUICAO_SOCIOS',
            'OUTRA_ENTRADA',
            'OUTRA_SAIDA',
            'INDEFINIDO'
        ));
