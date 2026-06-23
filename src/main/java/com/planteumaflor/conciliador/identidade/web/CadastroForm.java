package com.planteumaflor.conciliador.identidade.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Objeto que respalda o formulário de cadastro (tela 02 §5).
 *
 * É uma classe mutável (com getters/setters) de propósito: o Thymeleaf usa
 * {@code th:field} para ligação bidirecional, que espera acessores JavaBean.
 * Pertence à borda web, carrega as anotações de Bean Validation e nunca
 * atravessa para o domínio.
 */
public class CadastroForm {

    @NotBlank
    private String nomeEmpresa;

    private String cnpj;

    @NotBlank
    private String nomeResponsavel;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres.")
    private String senha;

    @NotBlank
    private String confirmarSenha;

    public boolean senhasConferem() {
        return senha != null && senha.equals(confirmarSenha);
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmarSenha() {
        return confirmarSenha;
    }

    public void setConfirmarSenha(String confirmarSenha) {
        this.confirmarSenha = confirmarSenha;
    }
}
