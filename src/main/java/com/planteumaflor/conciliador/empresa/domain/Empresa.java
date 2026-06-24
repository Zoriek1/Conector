package com.planteumaflor.conciliador.empresa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Empresa — raiz de isolamento dos dados (Backend §5.0).
 *
 * Construção controlada pela fábrica {@link #nova(String, String)}: o construtor
 * vazio existe só para o JPA. O {@code id} é gerado pelo Hibernate na inserção
 * ({@code @UuidGenerator}), então após {@code save()} a aplicação já conhece o id.
 */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "cnpj")
    private String cnpj;

    protected Empresa() {
        // exigido pelo JPA
    }

    public static Empresa nova(String nome, String cnpj) {
        Empresa empresa = new Empresa();
        empresa.nome = exigirTexto(nome, "nome da empresa");
        empresa.cnpj = (cnpj == null || cnpj.isBlank()) ? null : cnpj.strip();
        return empresa;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor.strip();
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    /** CNPJ bruto (pode ser nulo no v1). O mascaramento para exibição é da camada
     * de aplicação — a entidade não decide formatação (tela 09 §4). */
    public String getCnpj() {
        return cnpj;
    }
}
