package com.planteumaflor.conciliador.transacao.domain;

import java.util.Optional;
import java.util.UUID;

/** Porta de persistência; toda leitura individual exige o tenant. */
public interface TransacaoRepository {

    Transacao salvar(Transacao transacao);

    Optional<Transacao> buscarPorId(UUID empresaId, UUID transacaoId);

    boolean existePorOrigem(UUID empresaId, String pluggyTransactionId);
}
