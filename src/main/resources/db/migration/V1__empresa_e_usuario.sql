-- V1: identidade e tenant (Backend §5.0, §7.1)
-- Empresa é a raiz de isolamento; Usuario referencia exatamente uma empresa no v1.
-- gen_random_uuid() é nativo do PostgreSQL 13+ (sem extensão).

CREATE TABLE empresa (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        TEXT NOT NULL,
    cnpj        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE usuario (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id  UUID NOT NULL REFERENCES empresa(id),
    nome        TEXT NOT NULL,
    email       TEXT NOT NULL,
    senha_hash  TEXT NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- e-mail normalizado é único entre usuários (Backend §5.0)
    CONSTRAINT uq_usuario_email   UNIQUE (email),
    -- no v1 há exatamente um usuário por empresa
    CONSTRAINT uq_usuario_empresa UNIQUE (empresa_id)
);
