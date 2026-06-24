package com.planteumaflor.conciliador.onboarding.application;

import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;

import java.util.UUID;

/**
 * Consulta a etapa atual do onboarding de uma empresa.
 *
 * Fronteira de leitura: controllers e o handler de login dependem desta
 * interface, não da implementação nem do repositório.
 */
public interface ConsultarOnboarding {

    EtapaOnboarding etapaAtual(UUID empresaId);
}
