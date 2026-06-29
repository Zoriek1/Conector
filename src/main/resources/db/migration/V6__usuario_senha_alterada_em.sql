-- V6: registra quando a senha foi alterada pela última vez (tela 09 — Perfil §4).
-- Coluna NOT NULL com default now(); usuários já existentes recebem created_at.
-- A aplicação atualiza este campo na troca de senha; o default cobre a inserção.

ALTER TABLE usuario
    ADD COLUMN senha_alterada_em TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE usuario SET senha_alterada_em = created_at;
