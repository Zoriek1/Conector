package com.planteumaflor.conciliador.revisao.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Consulta paginada da empresa autenticada. */
public interface ConsultarFilaRevisao {

    Page<FilaRevisaoItemView> consultar(UUID empresaId, Pageable pageable);
}
