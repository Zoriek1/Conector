package com.planteumaflor.conciliador.cora.domain;

/** Categoria segura da última falha, sem payload ou mensagem externa. */
public enum TipoFalhaCora {
    AUTENTICACAO,
    COMUNICACAO,
    DADOS_INVALIDOS,
    INTERNO
}
