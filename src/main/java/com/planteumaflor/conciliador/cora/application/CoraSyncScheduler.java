package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.cora.domain.StatusIntegracaoCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumSet;
import java.util.UUID;

/** Worker periódico: um tenant por vez, com retry limitado e backoff. */
@Component
class CoraSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CoraSyncScheduler.class);

    private final IntegracaoCoraJpaRepository integracoes;
    private final SincronizarExtratoCora sincronizar;
    private final int maxTentativas;
    private final Duration atrasoRetry;

    CoraSyncScheduler(
            IntegracaoCoraJpaRepository integracoes,
            SincronizarExtratoCora sincronizar,
            ConciliadorProperties properties) {
        this.integracoes = integracoes;
        this.sincronizar = sincronizar;
        this.maxTentativas = properties.ingest().maxTentativas();
        this.atrasoRetry = properties.ingest().atrasoRetry();
    }

    @Scheduled(cron = "${conciliador.ingest.cron}", zone = "${conciliador.timezone}")
    void sincronizarIntegracoes() {
        var statusElegiveis = EnumSet.of(
                StatusIntegracaoCora.ATIVA, StatusIntegracaoCora.REQUER_ATENCAO);
        for (UUID empresaId : integracoes.findEmpresaIdsByStatusIn(statusElegiveis)) {
            sincronizarComRetry(empresaId);
        }
    }

    private void sincronizarComRetry(UUID empresaId) {
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                sincronizar.sincronizar(empresaId);
                return;
            } catch (RuntimeException e) {
                if (tentativa == maxTentativas) {
                    log.error("Sincronização Cora esgotou retries empresaId={} tentativas={}",
                            empresaId, maxTentativas);
                    return;
                }
                log.warn("Retry da sincronização Cora empresaId={} tentativa={}/{}",
                        empresaId, tentativa, maxTentativas);
                if (!aguardar(atrasoRetry.multipliedBy(tentativa))) {
                    return;
                }
            }
        }
    }

    private boolean aguardar(Duration atraso) {
        try {
            Thread.sleep(atraso);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
