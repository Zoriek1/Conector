package com.planteumaflor.conciliador.transacao.persistence;

import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTransacaoRepository extends Repository<Transacao, UUID> {

    Transacao save(Transacao transacao);

    Optional<Transacao> findByIdAndEmpresaId(UUID id, UUID empresaId);

    boolean existsByEmpresaIdAndFonteAndIdTransacaoExterna(
            UUID empresaId, FonteIntegracao fonte, String idTransacaoExterna);

    Page<Transacao> findByEmpresaId(UUID empresaId, Pageable pageable);
}
