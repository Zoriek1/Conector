package com.planteumaflor.conciliador.identidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario — autenticação, vinculada a exatamente uma empresa no v1 (Backend §5.0).
 *
 * Invariantes garantidas pela fábrica {@link #novo}: e-mail normalizado, senha
 * apenas como hash (a fábrica recebe o hash, nunca a senha em texto), usuário
 * criado ativo. {@code @Version} dá optimistic locking.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "nome")
    private String nome;

    @Column(name = "email")
    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(name = "ativo")
    private boolean ativo;

    /**
     * Quando a senha foi alterada pela última vez (tela 09 §4). Na inserção o
     * default {@code now()} do banco vale ({@code insertable = false}); a troca
     * de senha atualiza este campo via {@link #alterarSenha(String, Instant)}.
     */
    @Column(name = "senha_alterada_em", insertable = false)
    private Instant senhaAlteradaEm;

    @Version
    @Column(name = "version")
    private long version;

    protected Usuario() {
        // exigido pelo JPA
    }

    /**
     * Cria um usuário já com a senha codificada. Recebe o {@code senhaHash}
     * (nunca a senha em texto): a codificação acontece no caso de uso, antes.
     */
    public static Usuario novo(UUID empresaId, String nome, String email, String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.empresaId = Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        usuario.nome = exigirTexto(nome, "nome do responsável");
        usuario.email = normalizarEmail(email);
        usuario.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash é obrigatório");
        usuario.ativo = true;
        return usuario;
    }

    /** Normalização canônica do e-mail (minúsculas, sem espaços nas bordas). */
    public static String normalizarEmail(String email) {
        Objects.requireNonNull(email, "email é obrigatório");
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor.strip();
    }

    /**
     * Troca a senha (tela 09 §7). Recebe o hash já codificado e o instante da
     * troca — a codificação e o relógio ficam no caso de uso, nunca aqui.
     */
    public void alterarSenha(String novoHash, Instant quando) {
        this.senhaHash = Objects.requireNonNull(novoHash, "novoHash é obrigatório");
        this.senhaAlteradaEm = Objects.requireNonNull(quando, "quando é obrigatório");
    }

    /** Atualiza o nome do responsável (único campo editável no perfil — tela 09 §8). */
    public void alterarNomeResponsavel(String nome) {
        this.nome = exigirTexto(nome, "nome do responsável");
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getSenhaAlteradaEm() {
        return senhaAlteradaEm;
    }
}
