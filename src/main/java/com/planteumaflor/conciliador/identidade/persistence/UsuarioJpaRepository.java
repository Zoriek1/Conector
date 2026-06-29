package com.planteumaflor.conciliador.identidade.persistence;

import com.planteumaflor.conciliador.identidade.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Acesso Spring Data à entidade {@link Usuario}.
 *
 * {@code findByEmail} servirá ao carregamento por e-mail normalizado do
 * UserDetailsService (tela 01) no passo 2.
 */
public interface UsuarioJpaRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Carrega o usuário escopado pelo tenant (tela 09 §6): id de outra empresa
     * não retorna nada, equivalendo a 404 na camada web. */
    Optional<Usuario> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
