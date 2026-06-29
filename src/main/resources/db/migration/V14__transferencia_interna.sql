-- V14: metadados do pareamento de transferência interna de mesma titularidade.
-- A coluna transfer_par_id já existe (V3); aqui registramos como/quando o par
-- foi criado para habilitar o "Desfazer" e o aviso na UI.

ALTER TABLE transacao
    ADD COLUMN transfer_origem        TEXT,
    ADD COLUMN transfer_detectado_em  TIMESTAMPTZ;

ALTER TABLE transacao
    ADD CONSTRAINT ck_transacao_transfer_origem
        CHECK (transfer_origem IS NULL OR transfer_origem IN ('AUTOMATICA', 'MANUAL'));

-- Acelera a varredura de candidatos não pareados na detecção automática.
CREATE INDEX idx_transacao_transfer_candidato
    ON transacao (empresa_id, data, valor_liquido)
    WHERE transfer_par_id IS NULL;
