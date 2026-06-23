package com.planteumaflor.conciliador.pluggy.client;

import com.planteumaflor.conciliador.pluggy.application.PluggyConnectPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementação FAKE da conexão Pluggy (passo 3): simula o widget + callback e
 * devolve um id de item fictício, sem internet nem credenciais.
 *
 * Quando o Meu Pluggy real for integrado, esta classe é substituída por um
 * adapter de verdade (com @Profile / @Primary), sem mudar mais nada.
 */
@Component
class FakePluggyConnectAdapter implements PluggyConnectPort {

    @Override
    public String iniciarConexao(UUID empresaId) {
        return "fake-item-" + UUID.randomUUID();
    }
}
