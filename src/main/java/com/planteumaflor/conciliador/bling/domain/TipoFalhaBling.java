package com.planteumaflor.conciliador.bling.domain;

/** Categoria segura da última falha do Bling, sem payload nem token externo. */
public enum TipoFalhaBling {
    AUTENTICACAO,
    COMUNICACAO,
    DADOS_INVALIDOS,
    INTERNO
}
