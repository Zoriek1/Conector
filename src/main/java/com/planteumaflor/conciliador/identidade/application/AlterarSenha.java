package com.planteumaflor.conciliador.identidade.application;

import java.util.UUID;

/**
 * Caso de uso de troca de senha (tela 09 §7).
 *
 * O command carrega só os três campos do formulário. {@code usuarioId} e
 * {@code empresaId} vêm do principal autenticado, nunca do navegador. A senha
 * atual é verificada pelo {@code PasswordEncoder}; a nova nunca volta em view
 * model ou log.
 *
 * Observação: o v1 usa {@code String} para a senha por consistência com o
 * cadastro e com o binding de formulário do Spring. A migração para
 * {@code char[]} é um item de hardening já registrado em PROXIMOS-PASSOS.
 */
public interface AlterarSenha {

    void executar(UUID usuarioId, UUID empresaId, AlterarSenhaCommand comando);

    record AlterarSenhaCommand(String senhaAtual, String novaSenha, String confirmacao) {}
}
