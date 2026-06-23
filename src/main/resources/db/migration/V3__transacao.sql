-- V3: agregado financeiro e raiz do pipeline (Backend §5.1, §7.1).
-- Estados e enums usam os nomes Java persistidos por EnumType.STRING.

CREATE TABLE transacao (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id                  UUID NOT NULL REFERENCES empresa(id),

    pluggy_transaction_id       TEXT NOT NULL,
    pluggy_account_id           TEXT NOT NULL,
    conta_local                 TEXT NOT NULL,

    data                        DATE NOT NULL,
    valor_liquido               NUMERIC(14,2) NOT NULL,
    direcao                     TEXT NOT NULL,
    descricao_raw               TEXT,
    contraparte_doc             TEXT,
    e2e_id                      TEXT,

    classe                      TEXT NOT NULL DEFAULT 'INDEFINIDO',
    confianca                   NUMERIC(4,3) NOT NULL DEFAULT 0,
    justificativa_classificacao TEXT,
    motivo_revisao              TEXT,

    match_bling_tipo            TEXT,
    match_bling_id              TEXT,
    taxa_derivada               NUMERIC(14,2),

    transfer_par_id             UUID,
    estado                      TEXT NOT NULL DEFAULT 'INGERIDO',
    bling_bordero_id            TEXT,
    ofx_lote_id                 TEXT,

    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_transacao_pluggy
        UNIQUE (empresa_id, pluggy_transaction_id),
    CONSTRAINT uq_transacao_empresa_id
        UNIQUE (empresa_id, id),
    CONSTRAINT fk_transacao_transfer_mesma_empresa
        FOREIGN KEY (empresa_id, transfer_par_id)
        REFERENCES transacao (empresa_id, id),
    CONSTRAINT ck_transacao_valor_positivo
        CHECK (valor_liquido > 0),
    CONSTRAINT ck_transacao_confianca
        CHECK (confianca >= 0 AND confianca <= 1),
    CONSTRAINT ck_transacao_taxa
        CHECK (taxa_derivada IS NULL OR taxa_derivada >= 0),
    CONSTRAINT ck_transacao_direcao
        CHECK (direcao IN ('CREDITO', 'DEBITO')),
    CONSTRAINT ck_transacao_classe
        CHECK (classe IN (
            'CREDITO_VENDA',
            'TRANSFERENCIA_INTERNA',
            'DEBITO_DESPESA',
            'PRO_LABORE',
            'INDEFINIDO'
        )),
    CONSTRAINT ck_transacao_estado
        CHECK (estado IN (
            'INGERIDO',
            'CLASSIFICADO',
            'EM_REVISAO',
            'AGUARDANDO_ESCRITA_API',
            'ESCRITO_API',
            'EM_LOTE_OFX',
            'CONCILIADO',
            'FALHA'
        )),
    CONSTRAINT ck_transacao_match_tipo
        CHECK (match_bling_tipo IS NULL OR match_bling_tipo IN ('CONTA_RECEBER', 'CONTA_PAGAR'))
);

CREATE INDEX idx_transacao_estado
    ON transacao (empresa_id, estado);

CREATE INDEX idx_transacao_match
    ON transacao (empresa_id, data, valor_liquido, direcao);
