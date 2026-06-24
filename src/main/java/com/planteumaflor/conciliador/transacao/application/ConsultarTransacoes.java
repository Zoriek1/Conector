package com.planteumaflor.conciliador.transacao.application;

import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Porta pública de leitura das transações da empresa autenticada. */
public interface ConsultarTransacoes {

    Page<Transacao> listar(UUID empresaId, Pageable pageable);
}
