package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.classificacao.Classificador;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Busca o extrato no {@link CoraGateway}, ingere as transações novas
 * (idempotente por {@code idTransacaoExterna}) e classifica cada uma.
 */
@Service
class SincronizarExtratoCoraService implements SincronizarExtratoCora {

    private final IntegracaoCoraJpaRepository integracoes;
    private final CoraGateway gateway;
    private final CriptoService cripto;
    private final TransacaoRepository transacoes;
    private final Classificador classificador;
    private final Clock clock;
    private final int diasRetroativos;

    SincronizarExtratoCoraService(
            IntegracaoCoraJpaRepository integracoes,
            CoraGateway gateway,
            CriptoService cripto,
            TransacaoRepository transacoes,
            Classificador classificador,
            Clock clock,
            ConciliadorProperties properties) {
        this.integracoes = integracoes;
        this.gateway = gateway;
        this.cripto = cripto;
        this.transacoes = transacoes;
        this.classificador = classificador;
        this.clock = clock;
        this.diasRetroativos = properties.ingest().diasRetroativos();
    }

    @Override
    @Transactional
    public void sincronizar(UUID empresaId) {
        IntegracaoCora integracao = integracoes.findByEmpresaId(empresaId)
                .orElseThrow(() -> new IllegalStateException(
                        "empresa não tem integração Cora cadastrada"));

        CoraGateway.CredenciaisCora credenciais = new CoraGateway.CredenciaisCora(
                cripto.decifrar(integracao.getClientIdCifrado()),
                cripto.decifrar(integracao.getCertificadoCifrado()),
                cripto.decifrar(integracao.getChavePrivadaCifrada()));

        LocalDate hoje = LocalDate.now(clock);
        LocalDate de = hoje.minusDays(diasRetroativos);
        List<CoraGateway.LancamentoExtrato> lancamentos = gateway.extrato(credenciais, de, hoje);

        String contaIdExterno = integracao.getContaIdExterno();
        for (CoraGateway.LancamentoExtrato lancamento : lancamentos) {
            contaIdExterno = lancamento.idContaExterna();
            if (transacoes.existePorOrigem(empresaId, FonteIntegracao.CORA, lancamento.idTransacaoExterna())) {
                continue;
            }
            Transacao transacao = Transacao.ingerida(paraDados(empresaId, lancamento));
            classificador.classificar(transacao);
            transacoes.salvar(transacao);
        }

        Instant agora = clock.instant();
        integracao.registrarSincronizacao(contaIdExterno, agora);
        integracoes.save(integracao);
    }

    private DadosTransacao paraDados(UUID empresaId, CoraGateway.LancamentoExtrato lancamento) {
        Direcao direcao = lancamento.valor().signum() < 0 ? Direcao.DEBITO : Direcao.CREDITO;
        return new DadosTransacao(
                empresaId,
                FonteIntegracao.CORA,
                lancamento.idTransacaoExterna(),
                lancamento.idContaExterna(),
                "cora",
                lancamento.data(),
                lancamento.valor().abs(),
                direcao,
                lancamento.descricao(),
                lancamento.contraparteDoc(),
                lancamento.e2eId());
    }
}
