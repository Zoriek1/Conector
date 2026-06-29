package com.planteumaflor.conciliador.revisao.application;

/** Transação inexistente ou pertencente a outra empresa (traduzido para 404). */
public class RecursoNaoEncontrado extends RuntimeException {

    public RecursoNaoEncontrado(String mensagem) {
        super(mensagem);
    }
}
