package com.planteumaflor.conciliador.identidade.application;

/**
 * E-mail já existe. É uma RuntimeException para garantir rollback da transação
 * de cadastro. A mensagem ao usuário final é genérica (sem confirmar que o
 * e-mail existe) — a tradução acontece no controller.
 */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("e-mail já cadastrado");
    }
}
