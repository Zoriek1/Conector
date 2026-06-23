package com.planteumaflor.conciliador.identidade.security;

import com.planteumaflor.conciliador.identidade.domain.Usuario;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o Spring Security e a nossa identidade: carrega o usuário por
 * e-mail normalizado (tela 01).
 *
 * O e-mail é normalizado antes da consulta para casar com o que foi gravado no
 * cadastro. Se não encontrar, lança {@link UsernameNotFoundException} — que o
 * Spring converte em erro genérico de credenciais, sem revelar se o e-mail
 * existe.
 */
@Service
class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioJpaRepository usuarios;

    UsuarioDetailsService(UsuarioJpaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = Usuario.normalizarEmail(username);
        return usuarios.findByEmail(email)
                .map(UsuarioPrincipal::de)
                .orElseThrow(() -> new UsernameNotFoundException("credenciais inválidas"));
    }
}
