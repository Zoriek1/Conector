package com.planteumaflor.conciliador.identidade.application;

import com.planteumaflor.conciliador.empresa.domain.Empresa;
import com.planteumaflor.conciliador.empresa.persistence.EmpresaJpaRepository;
import com.planteumaflor.conciliador.identidade.domain.Usuario;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Implementação dos casos de uso do perfil (tela 09).
 *
 * Todas as operações são escopadas pelo tenant: o usuário é carregado por
 * {@code id + empresa_id}, então um id de outra empresa simplesmente não existe
 * aqui. A senha é codificada/verificada via {@link PasswordEncoder}; o instante
 * da troca vem do {@link Clock} injetado, nunca de {@code Instant.now()} direto.
 *
 * Visibilidade de pacote: o mundo externo enxerga só as interfaces.
 */
@Service
class PerfilService implements ConsultarPerfil, AlterarSenha, AtualizarPerfil {

    /** Política mínima do v1; alinhada com a dica de UI ("ao menos 8 caracteres"). */
    private static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioJpaRepository usuarios;
    private final EmpresaJpaRepository empresas;
    private final PasswordEncoder encoder;
    private final Clock clock;

    PerfilService(UsuarioJpaRepository usuarios, EmpresaJpaRepository empresas,
                  PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilView consultar(UUID usuarioId, UUID empresaId) {
        Usuario usuario = carregar(usuarioId, empresaId);
        Empresa empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new NoSuchElementException("empresa do tenant não encontrada"));
        return montarView(empresa, usuario);
    }

    @Override
    @Transactional
    public void executar(UUID usuarioId, UUID empresaId, AlterarSenhaCommand comando) {
        Usuario usuario = carregar(usuarioId, empresaId);

        if (!encoder.matches(comando.senhaAtual(), usuario.getSenhaHash())) {
            throw new SenhaAtualIncorretaException();
        }
        validarNovaSenha(comando);

        usuario.alterarSenha(encoder.encode(comando.novaSenha()), Instant.now(clock));
        usuarios.save(usuario);
    }

    @Override
    @Transactional
    public PerfilView executar(UUID usuarioId, UUID empresaId, AtualizarDadosCommand comando) {
        Usuario usuario = carregar(usuarioId, empresaId);
        usuario.alterarNomeResponsavel(comando.nomeResponsavel());
        usuarios.save(usuario);

        Empresa empresa = empresas.findById(empresaId)
                .orElseThrow(() -> new NoSuchElementException("empresa do tenant não encontrada"));
        return montarView(empresa, usuario);
    }

    private Usuario carregar(UUID usuarioId, UUID empresaId) {
        return usuarios.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new NoSuchElementException("usuário não encontrado no tenant"));
    }

    private void validarNovaSenha(AlterarSenhaCommand comando) {
        String nova = comando.novaSenha();
        if (nova == null || nova.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException("nova senha deve ter ao menos "
                    + TAMANHO_MINIMO_SENHA + " caracteres");
        }
        if (!nova.equals(comando.confirmacao())) {
            throw new IllegalArgumentException("confirmação não confere");
        }
    }

    private PerfilView montarView(Empresa empresa, Usuario usuario) {
        return new PerfilView(
                empresa.getNome(),
                mascararCnpj(empresa.getCnpj()),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSenhaAlteradaEm());
    }

    /** Mostra só os 4 últimos dígitos do CNPJ; o resto vira "•" (tela 09 §4). */
    private static String mascararCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return "—";
        }
        String digitos = cnpj.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return "•".repeat(Math.max(digitos.length(), 1));
        }
        String visiveis = digitos.substring(digitos.length() - 4);
        return "•".repeat(digitos.length() - 4) + visiveis;
    }
}
