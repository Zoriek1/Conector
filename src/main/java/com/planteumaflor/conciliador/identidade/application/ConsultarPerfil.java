package com.planteumaflor.conciliador.identidade.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso de leitura do perfil (tela 09 §2).
 *
 * Retorna apenas dados seguros para exibição — nunca hash de senha, tokens de
 * integração ou IDs editáveis de tenant. O {@code cnpjMascarado} já vem pronto
 * para a view; a entidade {@code Empresa} não é exposta.
 */
public interface ConsultarPerfil {

    PerfilView consultar(UUID usuarioId, UUID empresaId);

    record PerfilView(
            String nomeEmpresa,
            String cnpjMascarado,
            String nomeResponsavel,
            String email,
            Instant senhaAlteradaEm
    ) {}
}
