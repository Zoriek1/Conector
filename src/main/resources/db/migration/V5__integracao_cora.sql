-- V5: integração direta com a API do Cora (OAuth client-credentials + mTLS).
-- Uma conexão por empresa. As credenciais (clientId, certificado, chave
-- privada) são cifradas em aplicação (CriptoService) antes de chegar aqui.

CREATE TABLE integracao_cora (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id              UUID NOT NULL REFERENCES empresa(id),
    status                  TEXT NOT NULL,            -- NAO_CONECTADA | ATIVA | REQUER_ATENCAO | DESCONECTADA
    client_id_cifrado       TEXT NOT NULL,
    certificado_cifrado     TEXT NOT NULL,
    chave_privada_cifrada   TEXT NOT NULL,
    conta_id_externo        TEXT,
    conectado_em            TIMESTAMPTZ NOT NULL,
    ultima_sincronizacao    TIMESTAMPTZ,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- uma conexão Cora por empresa
    CONSTRAINT uq_integracao_cora_empresa UNIQUE (empresa_id)
);

CREATE INDEX idx_integracao_cora_empresa ON integracao_cora (empresa_id, status);
