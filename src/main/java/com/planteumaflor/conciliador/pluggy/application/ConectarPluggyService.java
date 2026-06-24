package com.planteumaflor.conciliador.pluggy.application;

import com.planteumaflor.conciliador.pluggy.domain.IntegracaoPluggy;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orquestra a conexão: pede ao {@link PluggyConnectPort} para conectar (no fake,
 * só devolve um item id) e persiste a {@link IntegracaoPluggy} ATIVA da empresa.
 *
 * Idempotente no v1: uma conexão Meu Pluggy por empresa. Se já existe, não faz
 * nada (clicar duas vezes não duplica).
 */
@Service
class ConectarPluggyService implements ConectarPluggy {

    private final PluggyConnectPort pluggy;
    private final IntegracaoPluggyJpaRepository integracoes;

    ConectarPluggyService(PluggyConnectPort pluggy, IntegracaoPluggyJpaRepository integracoes) {
        this.pluggy = pluggy;
        this.integracoes = integracoes;
    }

    @Override
    @Transactional
    public void conectar(UUID empresaId) {
        if (integracoes.findByEmpresaId(empresaId).isPresent()) {
            return; // já conectado
        }
        String pluggyItemId = pluggy.iniciarConexao(empresaId);
        integracoes.save(IntegracaoPluggy.conectada(empresaId, pluggyItemId));
    }
}
