-- Contas bancárias descobertas/cadastradas por conector.
-- A conta é o eixo comum para filtros, sincronização e lotes OFX.

CREATE TABLE conta_bancaria (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id          UUID NOT NULL REFERENCES empresa(id),
    fonte               TEXT NOT NULL,
    id_conta_externa    TEXT NOT NULL,
    nome                TEXT NOT NULL,
    banco_codigo        TEXT,
    agencia             TEXT,
    numero              TEXT,
    digito              TEXT,
    tipo                TEXT NOT NULL,
    ativa               BOOLEAN NOT NULL DEFAULT TRUE,
    ultima_sincronizacao TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_conta_bancaria_origem
        UNIQUE (empresa_id, fonte, id_conta_externa),
    CONSTRAINT ck_conta_bancaria_fonte
        CHECK (fonte IN ('CORA', 'PLUGGY')),
    CONSTRAINT ck_conta_bancaria_tipo
        CHECK (tipo IN ('CORRENTE', 'POUPANCA', 'PAGAMENTO', 'CARTAO_CREDITO', 'OUTRA'))
);

CREATE INDEX idx_conta_bancaria_empresa
    ON conta_bancaria (empresa_id, ativa);
