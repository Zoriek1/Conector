package com.planteumaflor.conciliador.identidade.security;

import com.planteumaflor.conciliador.identidade.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Representa o usuário autenticado na sessão (implementa {@link UserDetails}).
 *
 * Além do contrato do Spring Security, expõe {@code usuarioId} e {@code empresaId}
 * imutáveis — é daqui que os casos de uso obtêm o tenant, NUNCA de um parâmetro
 * enviado pelo navegador (Backend §5.0, tela 01).
 *
 * O hash da senha não tem getter próprio: só é exposto via {@link #getPassword()}
 * para o Spring conferir no login, e nunca deve aparecer em view model ou log.
 */
public final class UsuarioPrincipal implements UserDetails {

    private final UUID usuarioId;
    private final UUID empresaId;
    private final String email;
    private final String senhaHash;
    private final boolean ativo;

    public UsuarioPrincipal(UUID usuarioId, UUID empresaId, String email, String senhaHash, boolean ativo) {
        this.usuarioId = usuarioId;
        this.empresaId = empresaId;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
    }

    public static UsuarioPrincipal de(Usuario usuario) {
        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getEmpresaId(),
                usuario.getEmail(),
                usuario.getSenhaHash(),
                usuario.isAtivo());
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public UUID empresaId() {
        return empresaId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // No v1 há um único perfil operacional (Backend §11).
        return List.of(new SimpleGrantedAuthority("ROLE_OPERADOR"));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Usuário desativado não autentica — e a mensagem ao usuário é genérica.
        return ativo;
    }
}
