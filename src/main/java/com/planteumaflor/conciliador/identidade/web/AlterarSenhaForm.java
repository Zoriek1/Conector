package com.planteumaflor.conciliador.identidade.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulário de troca de senha (tela 09 §7). O controller valida o formato
 * básico; a verificação da senha atual e a política completa ficam no caso de
 * uso. As senhas nunca são reapresentadas no HTML.
 */
public class AlterarSenhaForm {

    @NotBlank(message = "Informe a senha atual.")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 8, message = "A nova senha deve ter ao menos 8 caracteres.")
    private String novaSenha;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmacao;

    public boolean novaSenhaConfere() {
        return novaSenha != null && novaSenha.equals(confirmacao);
    }

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacao() {
        return confirmacao;
    }

    public void setConfirmacao(String confirmacao) {
        this.confirmacao = confirmacao;
    }
}
