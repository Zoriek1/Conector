package com.planteumaflor.conciliador.onboarding.application;

import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;
import com.planteumaflor.conciliador.pluggy.domain.StatusIntegracao;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deriva a etapa do onboarding do estado persistido: se a empresa tem uma
 * integração Pluggy ATIVA, está concluído; senão, falta conectar.
 *
 * Visibilidade de pacote: o mundo externo enxerga só {@link ConsultarOnboarding}.
 */
@Service
class OnboardingService implements ConsultarOnboarding {

    private final IntegracaoPluggyJpaRepository integracoes;

    OnboardingService(IntegracaoPluggyJpaRepository integracoes) {
        this.integracoes = integracoes;
    }

    @Override
    public EtapaOnboarding etapaAtual(UUID empresaId) {
        boolean pluggyAtivo = integracoes.existsByEmpresaIdAndStatus(empresaId, StatusIntegracao.ATIVA);
        return pluggyAtivo ? EtapaOnboarding.CONCLUIDO : EtapaOnboarding.PLUGGY_PENDENTE;
    }
}
