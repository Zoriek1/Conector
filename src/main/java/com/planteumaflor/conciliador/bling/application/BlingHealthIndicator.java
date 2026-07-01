package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.bling.domain.StatusBling;
import com.planteumaflor.conciliador.bling.persistence.BlingTokenJpaRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Saúde operacional do Bling no Actuator (Backend §9.2): se algum refresh token
 * foi revogado ({@code DESCONECTADA}), a empresa precisa reconectar e as
 * escritas estão paradas. Reportado como DOWN no agregado {@code /health}, sem
 * afetar as probes de liveness/readiness (grupos próprios).
 *
 * Não expõe empresa nem token — apenas contagens.
 */
@Component("bling")
class BlingHealthIndicator implements HealthIndicator {

    private final BlingTokenJpaRepository tokens;

    BlingHealthIndicator(BlingTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public Health health() {
        long desconectadas = tokens.countByStatus(StatusBling.DESCONECTADA);
        long requeremAtencao = tokens.countByStatus(StatusBling.REQUER_ATENCAO);
        Health.Builder builder = desconectadas > 0 ? Health.down() : Health.up();
        return builder
                .withDetail("empresasDesconectadas", desconectadas)
                .withDetail("empresasRequeremAtencao", requeremAtencao)
                .build();
    }
}
