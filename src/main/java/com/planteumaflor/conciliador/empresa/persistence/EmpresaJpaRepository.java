package com.planteumaflor.conciliador.empresa.persistence;

import com.planteumaflor.conciliador.empresa.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Acesso Spring Data à entidade {@link Empresa}.
 *
 * No esqueleto é um {@code JpaRepository} direto. Quando os casos de uso
 * surgirem, a porta de domínio ({@code EmpresaRepository}) e este adapter de
 * persistência serão separados conforme Backend §4.
 */
public interface EmpresaJpaRepository extends JpaRepository<Empresa, UUID> {
}
