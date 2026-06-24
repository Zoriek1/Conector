package com.planteumaflor.conciliador.identidade.application;

import com.planteumaflor.conciliador.identidade.application.ConsultarPerfil.PerfilView;

import java.util.UUID;

/**
 * Caso de uso de atualização de dados do perfil (tela 09 §8).
 *
 * No v1 só o nome do responsável é editável (allowlist explícita) — nome e CNPJ
 * da empresa são somente leitura para não impactar integrações e auditoria sem
 * fluxo próprio. Não existe update genérico de {@code Usuario}/{@code Empresa}.
 */
public interface AtualizarPerfil {

    PerfilView executar(UUID usuarioId, UUID empresaId, AtualizarDadosCommand comando);

    record AtualizarDadosCommand(String nomeResponsavel) {}
}
