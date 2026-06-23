-- V2: conexão Pluggy por empresa (Backend §5.0, tela 03).
-- Cada empresa usa o seu próprio Meu Pluggy. No v1 modelamos UMA conexão por
-- empresa (status-driven); o detalhamento por banco/conta vem depois.
--
-- As credenciais do Meu Pluggy (clientId/clientSecret, criptografadas) e os
-- ids de item/contas entram quando o adapter REAL substituir o fake.

CREATE TABLE integracao_pluggy (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id      UUID NOT NULL REFERENCES empresa(id),
    status          TEXT NOT NULL,            -- NAO_CONECTADA | ATIVA | REQUER_ATENCAO | DESCONECTADA
    pluggy_item_id  TEXT,                     -- id da conexão no Pluggy (fake por enquanto)
    conectado_em    TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- no v1, uma conexão Meu Pluggy por empresa
    CONSTRAINT uq_integracao_pluggy_empresa UNIQUE (empresa_id)
);

CREATE INDEX idx_integracao_pluggy_empresa ON integracao_pluggy (empresa_id, status);
