package com.planteumaflor.conciliador.pluggy.persistence;

import com.planteumaflor.conciliador.pluggy.domain.IntegracaoPluggy;
import com.planteumaflor.conciliador.pluggy.domain.StatusIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso Spring Data à integração Pluggy.
 *
 * As consultas são escopadas por {@code empresaId} (o tenant da sessão) —
 * isolamento por empresa (Backend §11).
 */
public interface IntegracaoPluggyJpaRepository extends JpaRepository<IntegracaoPluggy, UUID> {

    Optional<IntegracaoPluggy> findByEmpresaId(UUID empresaId);

    boolean existsByEmpresaIdAndStatus(UUID empresaId, StatusIntegracao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IntegracaoPluggy i where i.empresaId = :empresaId")
    Optional<IntegracaoPluggy> findByEmpresaIdForUpdate(@Param("empresaId") UUID empresaId);

    @Query("select i.empresaId from IntegracaoPluggy i where i.status in :status")
    List<UUID> findEmpresaIdsByStatusIn(@Param("status") Collection<StatusIntegracao> status);
}
