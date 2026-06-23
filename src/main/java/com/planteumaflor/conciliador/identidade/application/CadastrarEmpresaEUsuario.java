package com.planteumaflor.conciliador.identidade.application;

import java.util.UUID;

/**
 * Caso de uso: criar atomicamente uma empresa e seu único usuário (tela 02).
 *
 * É a fronteira entre a camada web e a aplicação: o controller depende desta
 * interface, não da implementação ({@code CadastroService}) nem do JPA.
 *
 * O command NÃO contém {@code empresaId} — ele é criado pelo caso de uso.
 */
public interface CadastrarEmpresaEUsuario {

    CadastroRealizado executar(CadastrarEmpresaCommand comando);

    record CadastrarEmpresaCommand(
            String nomeEmpresa,
            String cnpj,
            String nomeResponsavel,
            String email,
            String senha
    ) {}

    record CadastroRealizado(UUID empresaId, UUID usuarioId) {}
}
