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
}
