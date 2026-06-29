-- Lotes OFX gerados para upload manual no Bling.

CREATE TABLE lote_ofx (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id          UUID NOT NULL REFERENCES empresa(id),
    conta_bancaria_id   UUID NOT NULL REFERENCES conta_bancaria(id),
    data_inicio         DATE NOT NULL,
    data_fim            DATE NOT NULL,
    status              TEXT NOT NULL,
    nome_arquivo        TEXT NOT NULL,
    media_type          TEXT NOT NULL,
    tamanho_bytes       BIGINT NOT NULL,
    checksum_sha256     TEXT NOT NULL,
    conteudo            BYTEA NOT NULL,
    quantidade_itens    INTEGER NOT NULL,
    total_creditos      NUMERIC(14,2) NOT NULL,
    total_debitos       NUMERIC(14,2) NOT NULL,
    confirmado_em       TIMESTAMPTZ,
    observacao          TEXT,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_lote_ofx_status
        CHECK (status IN ('DISPONIVEL', 'UPLOAD_CONFIRMADO', 'CANCELADO')),
    CONSTRAINT ck_lote_ofx_periodo
        CHECK (data_inicio <= data_fim),
    CONSTRAINT ck_lote_ofx_quantidade
        CHECK (quantidade_itens >= 0),
    CONSTRAINT ck_lote_ofx_totais
        CHECK (total_creditos >= 0 AND total_debitos >= 0)
);

CREATE TABLE lote_ofx_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lote_ofx_id     UUID NOT NULL REFERENCES lote_ofx(id),
    transacao_id    UUID NOT NULL REFERENCES transacao(id),

    CONSTRAINT uq_lote_ofx_item_transacao UNIQUE (transacao_id)
);

CREATE INDEX idx_lote_ofx_empresa
    ON lote_ofx (empresa_id, status, created_at DESC);
