package com.planteumaflor.conciliador.identidade.application;

import java.util.UUID;

/**
 * Caso de uso de encerramento das demais sessões do usuário (tela 09 §9).
 *
 * Preserva a sessão atual (passada por id) e expira as outras. A resposta não
 * revela quantas/quais sessões existiam.
 */
public interface EncerrarOutrasSessoes {

    void executar(UUID usuarioId, String sessaoAtualId);
}
