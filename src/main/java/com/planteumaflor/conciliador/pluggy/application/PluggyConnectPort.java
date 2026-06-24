package com.planteumaflor.conciliador.pluggy.application;

import java.util.UUID;

/**
 * Porta para conectar o Meu Pluggy de uma empresa (tela 03 §8).
 *
 * O nosso código depende DESTA interface, não da implementação. Hoje quem a
 * cumpre é um fake controlável ({@code FakePluggyConnectAdapter}); amanhã, um
 * adapter real com o widget + a API do Pluggy. Trocar é trocar um bean.
 */
public interface PluggyConnectPort {

    /**
     * Executa a conexão (no real: widget + callback) e devolve o id do item
     * (a conexão) no Pluggy.
     */
    String iniciarConexao(UUID empresaId);
}
