package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import com.planteumaflor.conciliador.cora.domain.TipoFalhaCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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

    private static final Logger log = LoggerFactory.getLogger(SincronizarExtratoCoraService.class);

    private final IntegracaoCoraJpaRepository integracoes;
    private final CoraGateway gateway;
    private final CriptoService cripto;
    private final IngerirLancamentoCoraService ingerirLancamento;
    private final IntegracaoCoraSaudeService saude;
    private final Clock clock;
    private final int diasRetroativos;

    SincronizarExtratoCoraService(
            IntegracaoCoraJpaRepository integracoes,
            CoraGateway gateway,
            CriptoService cripto,
            IngerirLancamentoCoraService ingerirLancamento,
            IntegracaoCoraSaudeService saude,
            Clock clock,
            ConciliadorProperties properties) {
        this.integracoes = integracoes;
        this.gateway = gateway;
        this.cripto = cripto;
        this.ingerirLancamento = ingerirLancamento;
        this.saude = saude;
        this.clock = clock;
        this.diasRetroativos = properties.ingest().diasRetroativos();
    }

    @Override
    public void sincronizar(UUID empresaId) {
        IntegracaoCora integracao = integracoes.findByEmpresaId(empresaId)
                .orElseThrow(() -> new IllegalStateException(
                        "empresa não tem integração Cora cadastrada"));

        try {
            CoraGateway.CredenciaisCora credenciais = new CoraGateway.CredenciaisCora(
                    cripto.decifrar(integracao.getClientIdCifrado()),
                    cripto.decifrar(integracao.getCertificadoCifrado()),
                    cripto.decifrar(integracao.getChavePrivadaCifrada()));

            LocalDate hoje = LocalDate.now(clock);
            LocalDate de = hoje.minusDays(diasRetroativos);
            List<CoraGateway.LancamentoExtrato> lancamentos = gateway.extrato(credenciais, de, hoje);

            String contaIdExterno = integracao.getContaIdExterno();
            int inseridas = 0;
            for (CoraGateway.LancamentoExtrato lancamento : lancamentos) {
                contaIdExterno = lancamento.idContaExterna();
                if (ingerirLancamento.ingerir(empresaId, lancamento)) {
                    inseridas++;
                }
            }

            saude.registrarSucesso(empresaId, contaIdExterno, clock.instant());
            log.info("Sincronização Cora concluída empresaId={} recebidas={} inseridas={}",
                    empresaId, lancamentos.size(), inseridas);
        } catch (RuntimeException e) {
            TipoFalhaCora tipo = classificarFalha(e);
            saude.registrarFalha(empresaId, tipo, clock.instant());
            log.warn("Sincronização Cora falhou empresaId={} tipo={} causa={}",
                    empresaId, tipo, e.getClass().getSimpleName());
            throw e;
        }
    }

    private TipoFalhaCora classificarFalha(RuntimeException erro) {
        if (erro instanceof RestClientResponseException resposta
                && (resposta.getStatusCode().value() == 401
                || resposta.getStatusCode().value() == 403)) {
            return TipoFalhaCora.AUTENTICACAO;
        }
        if (erro instanceof RestClientException) {
            return TipoFalhaCora.COMUNICACAO;
        }
        if (erro instanceof IllegalArgumentException) {
            return TipoFalhaCora.DADOS_INVALIDOS;
        }
        return TipoFalhaCora.INTERNO;
    }
}
