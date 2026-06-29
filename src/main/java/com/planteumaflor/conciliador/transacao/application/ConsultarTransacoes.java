package com.planteumaflor.conciliador.transacao.application;

import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Porta pública de leitura das transações da empresa autenticada. */
public interface ConsultarTransacoes {

    /**
     * Lista as transações da empresa. Por padrão oculta as transferências internas
     * já pareadas; passe {@code incluirTransferencias} como verdadeiro para exibi-las.
     */
    Page<Transacao> listar(UUID empresaId, boolean incluirTransferencias, Pageable pageable);
}
