package com.planteumaflor.conciliador.bling.persistence;

import com.planteumaflor.conciliador.bling.domain.BlingToken;
import com.planteumaflor.conciliador.bling.domain.StatusBling;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Acesso Spring Data ao token Bling; consultas escopadas por empresa. */
public interface BlingTokenJpaRepository extends JpaRepository<BlingToken, UUID> {

    Optional<BlingToken> findByEmpresaId(UUID empresaId);

    /**
     * Carrega o token com lock pessimista na linha para serializar o refresh
     * (Backend §9.2): só uma renovação por empresa de cada vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BlingToken t where t.empresaId = :empresaId")
    Optional<BlingToken> findByEmpresaIdForUpdate(@Param("empresaId") UUID empresaId);

    long countByStatus(StatusBling status);
}
