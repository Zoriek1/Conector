-- Ajusta a constraint de classes para as categorias gerenciais da floricultura.

ALTER TABLE transacao
    DROP CONSTRAINT ck_transacao_classe;

ALTER TABLE transacao
    ADD CONSTRAINT ck_transacao_classe
        CHECK (classe IN (
            'CREDITO_VENDA',
            'RECEITA_FINANCEIRA',
            'ESTORNO_REEMBOLSO_RECEBIDO',
            'APORTE_SOCIO',
            'EMPRESTIMO_RECEBIDO',
            'APORTE_EMPRESTIMO',
            'TRANSFERENCIA_INTERNA',
            'FLORES_FOLHAGENS_PLANTAS',
            'EMBALAGENS_COMPLEMENTOS',
            'INSUMOS_JARDINAGEM',
            'FRETE_ENTREGAS',
            'OUTROS_CUSTOS_DIRETOS',
            'FORNECEDORES_CMV',
            'MARKETING_TRAFEGO',
            'MARKETING_VENDAS',
            'TAXAS_VENDAS',
            'COMISSOES_VENDA',
            'FERRAMENTAS_VENDA_ATENDIMENTO',
            'SALARIOS_ENCARGOS',
            'PRO_LABORE',
            'RETIRADA_DISTRIBUICAO_SOCIOS',
            'ALUGUEL_INFRA',
            'AGUA_ENERGIA_INTERNET_TELEFONE',
            'SISTEMAS_ASSINATURAS',
            'MANUTENCAO_LIMPEZA_MATERIAIS',
            'CONTABILIDADE_SERVICOS',
            'IMPOSTOS_TRIBUTOS',
            'TARIFAS_BANCARIAS',
            'JUROS_MULTAS',
            'TARIFAS_JUROS',
            'PAGAMENTO_EMPRESTIMO',
            'INVESTIMENTOS_EQUIPAMENTOS',
            'DEBITO_DESPESA',
            'FRETE_LOGISTICA',
            'OUTRA_ENTRADA',
            'OUTRA_SAIDA',
            'INDEFINIDO'
        ));
