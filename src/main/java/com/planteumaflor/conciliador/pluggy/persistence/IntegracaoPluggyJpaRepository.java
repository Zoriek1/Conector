package com.planteumaflor.conciliador.pluggy.persistence;

import com.planteumaflor.conciliador.pluggy.domain.IntegracaoPluggy;
import com.planteumaflor.conciliador.pluggy.domain.StatusIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
