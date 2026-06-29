-- Saúde operacional da integração Cora. A falha persistida é categórica para
-- não armazenar respostas HTTP, certificados, tokens ou outros dados sensíveis.

ALTER TABLE integracao_cora
    ADD COLUMN ultima_falha_em TIMESTAMPTZ,
    ADD COLUMN ultima_falha_tipo TEXT,
    ADD COLUMN falhas_consecutivas INTEGER NOT NULL DEFAULT 0;

ALTER TABLE integracao_cora
    ADD CONSTRAINT ck_integracao_cora_falhas_consecutivas
        CHECK (falhas_consecutivas >= 0),
    ADD CONSTRAINT ck_integracao_cora_ultima_falha_tipo
        CHECK (ultima_falha_tipo IS NULL OR ultima_falha_tipo IN (
            'AUTENTICACAO', 'COMUNICACAO', 'DADOS_INVALIDOS', 'INTERNO'
        ));
