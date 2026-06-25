package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import com.planteumaflor.conciliador.cora.domain.TipoFalhaCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Persiste saúde em transação independente da chamada ao provedor. */
@Service
class IntegracaoCoraSaudeService {

    private final IntegracaoCoraJpaRepository integracoes;

    IntegracaoCoraSaudeService(IntegracaoCoraJpaRepository integracoes) {
        this.integracoes = integracoes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void registrarSucesso(UUID empresaId, String contaIdExterno, Instant agora) {
        carregar(empresaId).registrarSincronizacao(contaIdExterno, agora);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void registrarFalha(UUID empresaId, TipoFalhaCora tipo, Instant agora) {
        carregar(empresaId).registrarFalha(tipo, agora);
    }

    private IntegracaoCora carregar(UUID empresaId) {
        return integracoes.findByEmpresaIdForUpdate(empresaId)
                .orElseThrow(() -> new IllegalStateException(
                        "empresa não tem integração Cora cadastrada"));
    }
}
