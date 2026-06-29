package com.planteumaflor.conciliador.cora.persistence;

import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.planteumaflor.conciliador.cora.domain.StatusIntegracaoCora;

/** Acesso Spring Data à integração Cora; consultas escopadas por empresa. */
public interface IntegracaoCoraJpaRepository extends JpaRepository<IntegracaoCora, UUID> {

    Optional<IntegracaoCora> findByEmpresaId(UUID empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IntegracaoCora i where i.empresaId = :empresaId")
    Optional<IntegracaoCora> findByEmpresaIdForUpdate(@Param("empresaId") UUID empresaId);

    @Query("select i.empresaId from IntegracaoCora i where i.status in :status")
    List<UUID> findEmpresaIdsByStatusIn(@Param("status") Collection<StatusIntegracaoCora> status);
}
