package com.planteumaflor.conciliador.revisao.application;

/**
 * A versão enviada pelo formulário não corresponde à persistida — outra edição
 * ocorreu nesse meio-tempo (traduzido para 409).
 */
public class ConflitoDeVersao extends RuntimeException {

    public ConflitoDeVersao(String mensagem) {
        super(mensagem);
    }
}
