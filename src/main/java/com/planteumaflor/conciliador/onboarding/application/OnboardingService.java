package com.planteumaflor.conciliador.onboarding.application;

import com.planteumaflor.conciliador.conta.persistence.ContaBancariaJpaRepository;
import com.planteumaflor.conciliador.cora.domain.StatusIntegracaoCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;
import com.planteumaflor.conciliador.pluggy.domain.StatusIntegracao;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deriva a etapa do onboarding do estado persistido: qualquer conector ativo
 * (Cora ou Pluggy) + uma conta ativa libera a operação.
 *
 * Visibilidade de pacote: o mundo externo enxerga só {@link ConsultarOnboarding}.
 */
@Service
class OnboardingService implements ConsultarOnboarding {

    private final IntegracaoPluggyJpaRepository pluggy;
    private final IntegracaoCoraJpaRepository cora;
    private final ContaBancariaJpaRepository contas;

    OnboardingService(
            IntegracaoPluggyJpaRepository pluggy,
            IntegracaoCoraJpaRepository cora,
            ContaBancariaJpaRepository contas) {
        this.pluggy = pluggy;
        this.cora = cora;
        this.contas = contas;
    }

    @Override
    public EtapaOnboarding etapaAtual(UUID empresaId) {
        boolean conectorAtivo = pluggy.existsByEmpresaIdAndStatus(empresaId, StatusIntegracao.ATIVA)
                || cora.findByEmpresaId(empresaId)
                .map(i -> i.getStatus() == StatusIntegracaoCora.ATIVA)
                .orElse(false);
        if (!conectorAtivo) {
            return EtapaOnboarding.INTEGRACOES_PENDENTES;
        }
        return contas.existsByEmpresaIdAndAtivaTrue(empresaId)
                ? EtapaOnboarding.CONCLUIDO
                : EtapaOnboarding.CONTAS_PENDENTES;
    }
}
