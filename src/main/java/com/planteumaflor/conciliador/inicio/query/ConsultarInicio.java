package com.planteumaflor.conciliador.inicio.query;

import java.util.UUID;

public interface ConsultarInicio {

    InicioView consultar(UUID empresaId);
}
