-- Credenciais do Meu Pluggy por empresa. São cifradas pela aplicação antes de
-- chegar ao banco; o .env não guarda credenciais Pluggy.

ALTER TABLE integracao_pluggy
    ADD COLUMN client_id_cifrado TEXT,
    ADD COLUMN client_secret_cifrado TEXT,
    ADD COLUMN ultima_sincronizacao TIMESTAMPTZ,
    ADD COLUMN ultima_falha_em TIMESTAMPTZ,
    ADD COLUMN ultima_falha_tipo TEXT,
    ADD COLUMN falhas_consecutivas INTEGER NOT NULL DEFAULT 0;

ALTER TABLE integracao_pluggy
    ADD CONSTRAINT ck_integracao_pluggy_falhas_consecutivas
        CHECK (falhas_consecutivas >= 0),
    ADD CONSTRAINT ck_integracao_pluggy_ultima_falha_tipo
        CHECK (ultima_falha_tipo IS NULL OR ultima_falha_tipo IN (
            'AUTENTICACAO', 'COMUNICACAO', 'DADOS_INVALIDOS', 'INTERNO'
        ));
