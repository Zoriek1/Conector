package com.planteumaflor.conciliador.conta.persistence;

import com.planteumaflor.conciliador.conta.domain.ContaBancaria;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaBancariaJpaRepository extends JpaRepository<ContaBancaria, UUID> {

    List<ContaBancaria> findByEmpresaIdOrderByFonteAscNomeAsc(UUID empresaId);

    List<ContaBancaria> findByEmpresaIdAndAtivaTrueOrderByFonteAscNomeAsc(UUID empresaId);

    Optional<ContaBancaria> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<ContaBancaria> findByEmpresaIdAndFonteAndIdContaExterna(
            UUID empresaId, FonteIntegracao fonte, String idContaExterna);

    boolean existsByEmpresaIdAndAtivaTrue(UUID empresaId);
}
