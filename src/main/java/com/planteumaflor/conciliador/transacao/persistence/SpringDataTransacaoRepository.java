package com.planteumaflor.conciliador.transacao.persistence;

import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTransacaoRepository extends Repository<Transacao, UUID> {

    Transacao save(Transacao transacao);

    Optional<Transacao> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndPluggyTransactionId(UUID empresaId, String pluggyTransactionId);
}
