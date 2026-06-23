package com.planteumaflor.conciliador.empresa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Empresa — raiz de isolamento dos dados (Backend §5.0).
 *
 * Esqueleto: mapeamento mínimo só para validar o schema. Comportamento de
 * domínio e construção via fábrica entram no passo 2 (cadastro atômico).
 */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "cnpj")
    private String cnpj;

    protected Empresa() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
