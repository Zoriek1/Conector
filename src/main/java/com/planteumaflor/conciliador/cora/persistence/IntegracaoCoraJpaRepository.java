package com.planteumaflor.conciliador.cora.persistence;

import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Acesso Spring Data à integração Cora; consultas escopadas por empresa. */
public interface IntegracaoCoraJpaRepository extends JpaRepository<IntegracaoCora, UUID> {

    Optional<IntegracaoCora> findByEmpresaId(UUID empresaId);
}
