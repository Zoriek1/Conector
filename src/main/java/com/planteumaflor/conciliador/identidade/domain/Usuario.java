package com.planteumaflor.conciliador.identidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * Usuario — autenticação, vinculada a exatamente uma empresa no v1 (Backend §5.0).
 *
 * Carrega {@code empresaId} como coluna (o tenant acompanha o registro). A senha
 * vive só como hash. {@code @Version} dá optimistic locking.
 *
 * Esqueleto: mapeamento mínimo. Métodos de domínio (ativar/desativar, trocar
 * senha) entram no passo 2.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
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

    @Version
    @Column(name = "version")
    private long version;

    protected Usuario() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
