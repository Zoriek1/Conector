package com.planteumaflor.conciliador.identidade.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Formulário de dados editáveis do perfil (tela 09 §8). No v1, apenas o nome do
 * responsável é editável — nome e CNPJ da empresa são somente leitura.
 */
public class AtualizarDadosForm {

    @NotBlank(message = "Informe o nome do responsável.")
    private String nomeResponsavel;

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }
}
