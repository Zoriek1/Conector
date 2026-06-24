package com.planteumaflor.conciliador.identidade.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mutações de domínio do {@link Usuario} usadas pelo perfil (tela 09). Unitário
 * puro — não depende de banco.
 */
class UsuarioTest {

    private Usuario novoUsuario() {
        return Usuario.novo(UUID.randomUUID(), "Ana", "ana@example.com", "{noop}hash-antigo");
    }

    @Test
    void alterarSenhaTrocaHashERegistraInstante() {
        Usuario usuario = novoUsuario();
        Instant quando = Instant.parse("2026-06-24T12:00:00Z");

        usuario.alterarSenha("{noop}hash-novo", quando);

        assertThat(usuario.getSenhaHash()).isEqualTo("{noop}hash-novo");
        assertThat(usuario.getSenhaAlteradaEm()).isEqualTo(quando);
    }

    @Test
    void alterarSenhaRejeitaValoresNulos() {
        Usuario usuario = novoUsuario();

        assertThatThrownBy(() -> usuario.alterarSenha(null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> usuario.alterarSenha("{noop}x", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void alterarNomeResponsavelAtualizaEExigeTexto() {
        Usuario usuario = novoUsuario();

        usuario.alterarNomeResponsavel("  Bruno  ");
        assertThat(usuario.getNome()).isEqualTo("Bruno");

        assertThatThrownBy(() -> usuario.alterarNomeResponsavel("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
