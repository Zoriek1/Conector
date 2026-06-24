-- V4: generaliza a origem da transação para suportar múltiplas fontes além do
-- Pluggy (ex.: integração direta com o Cora). V3 não é alterada.

ALTER TABLE transacao RENAME COLUMN pluggy_transaction_id TO id_transacao_externa;
ALTER TABLE transacao RENAME COLUMN pluggy_account_id TO id_conta_externa;

ALTER TABLE transacao ADD COLUMN fonte TEXT NOT NULL DEFAULT 'PLUGGY';
ALTER TABLE transacao ALTER COLUMN fonte DROP DEFAULT;

ALTER TABLE transacao
    DROP CONSTRAINT uq_transacao_pluggy;
ALTER TABLE transacao
    ADD CONSTRAINT uq_transacao_origem
        UNIQUE (empresa_id, fonte, id_transacao_externa);

ALTER TABLE transacao
    ADD CONSTRAINT ck_transacao_fonte
        CHECK (fonte IN ('PLUGGY', 'CORA'));
