package com.planteumaflor.conciliador.identidade.application;

/**
 * Senha atual informada não confere (tela 09 §7). RuntimeException para garantir
 * rollback. A mensagem ao usuário é controlada e o controller responde 422 sem
 * preservar os campos preenchidos.
 */
public class SenhaAtualIncorretaException extends RuntimeException {

    public SenhaAtualIncorretaException() {
        super("senha atual incorreta");
    }
}
