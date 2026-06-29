package com.planteumaflor.conciliador.revisao.query;

import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** Consulta paginada da empresa autenticada, escopada por estado. */
public interface ConsultarFilaRevisao {

    Page<FilaRevisaoItemView> consultar(UUID empresaId, EstadoTransacao estado, Pageable pageable);

    /** Item único da empresa, para fragmentos HTMX de linha. */
    Optional<FilaRevisaoItemView> consultarItem(UUID empresaId, UUID transacaoId);
}
