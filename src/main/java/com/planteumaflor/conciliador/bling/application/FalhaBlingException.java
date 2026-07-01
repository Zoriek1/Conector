package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.bling.domain.TipoFalhaBling;

/**
 * Falha ao falar com o Bling, já classificada em um {@link TipoFalhaBling}
 * seguro (sem vazar payload ou token). O client traduz erros HTTP para esta
 * exceção; os serviços a usam para registrar a saúde da integração.
 */
public class FalhaBlingException extends RuntimeException {

    private final TipoFalhaBling tipo;

    public FalhaBlingException(TipoFalhaBling tipo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
    }

    public FalhaBlingException(TipoFalhaBling tipo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.tipo = tipo;
    }

    public TipoFalhaBling tipo() {
        return tipo;
    }
}
