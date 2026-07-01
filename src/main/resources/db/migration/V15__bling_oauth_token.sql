-- V15: token OAuth 2.0 do Bling por empresa (Backend §9.2, Bling-API-v3 §6).
-- Um token por empresa. access_token e refresh_token são cifrados em aplicação
-- (CriptoService) antes de chegar aqui; nunca trafegam nem são logados em claro.

CREATE TABLE bling_oauth_token (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id              UUID NOT NULL REFERENCES empresa(id),
    status                  TEXT NOT NULL,            -- ATIVA | REQUER_ATENCAO | DESCONECTADA
    access_token_cifrado    TEXT NOT NULL,
    refresh_token_cifrado   TEXT NOT NULL,
    expira_em               TIMESTAMPTZ NOT NULL,     -- validade do access token
    conectado_em            TIMESTAMPTZ NOT NULL,
    ultima_renovacao        TIMESTAMPTZ,
    ultima_falha_em         TIMESTAMPTZ,
    ultima_falha_tipo       TEXT,                     -- AUTENTICACAO | COMUNICACAO | DADOS_INVALIDOS | INTERNO
    falhas_consecutivas     INTEGER NOT NULL DEFAULT 0,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- um token Bling por empresa (Backend §5.0)
    CONSTRAINT uq_bling_oauth_token_empresa UNIQUE (empresa_id)
);

CREATE INDEX idx_bling_oauth_token_empresa ON bling_oauth_token (empresa_id, status);
