package com.planteumaflor.conciliador.pluggy.application;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import com.planteumaflor.conciliador.conta.application.ContaBancariaService;
import com.planteumaflor.conciliador.conta.domain.TipoContaBancaria;
import com.planteumaflor.conciliador.pluggy.domain.IntegracaoPluggy;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PluggyIntegrationService {

    private final IntegracaoPluggyJpaRepository integracoes;
    private final PluggyGateway gateway;
    private final CriptoService cripto;
    private final ContaBancariaService contas;
    private final IngerirTransacaoPluggyService ingerirTransacao;
    private final Clock clock;
    private final int diasRetroativos;

    public PluggyIntegrationService(
            IntegracaoPluggyJpaRepository integracoes,
            PluggyGateway gateway,
            CriptoService cripto,
            ContaBancariaService contas,
            IngerirTransacaoPluggyService ingerirTransacao,
            Clock clock,
            ConciliadorProperties properties) {
        this.integracoes = integracoes;
        this.gateway = gateway;
        this.cripto = cripto;
        this.contas = contas;
        this.ingerirTransacao = ingerirTransacao;
        this.clock = clock;
        this.diasRetroativos = properties.ingest().diasRetroativos();
    }

    @Transactional
    public void salvarCredenciais(UUID empresaId, String clientId, String clientSecret) {
        gateway.criarApiKey(new PluggyGateway.CredenciaisPluggy(clientId, clientSecret));
        Instant agora = clock.instant();
        IntegracaoPluggy integracao = integracoes.findByEmpresaId(empresaId)
                .map(existente -> {
                    existente.atualizarCredenciais(
                            cripto.cifrar(clientId), cripto.cifrar(clientSecret), agora);
                    return existente;
                })
                .orElseGet(() -> IntegracaoPluggy.comCredenciais(
                        empresaId, cripto.cifrar(clientId), cripto.cifrar(clientSecret), agora));
        integracoes.save(integracao);
    }

    @Transactional(readOnly = true)
    public PluggyGateway.ConnectToken criarConnectToken(UUID empresaId) {
        IntegracaoPluggy integracao = integracoes.findByEmpresaId(empresaId)
                .orElseThrow(() -> new IllegalStateException("credenciais Pluggy não cadastradas"));
        String apiKey = apiKey(integracao);
        return gateway.criarConnectToken(apiKey, empresaId.toString());
    }

    @Transactional
    public void confirmarItem(UUID empresaId, String pluggyItemId) {
        IntegracaoPluggy integracao = integracoes.findByEmpresaId(empresaId)
                .orElseThrow(() -> new IllegalStateException("credenciais Pluggy não cadastradas"));
        integracao.confirmarItem(pluggyItemId, clock.instant());
        descobrirContas(integracao);
    }

    @Transactional
    public int sincronizar(UUID empresaId) {
        IntegracaoPluggy integracao = integracoes.findByEmpresaIdForUpdate(empresaId)
                .orElseThrow(() -> new IllegalStateException("credenciais Pluggy não cadastradas"));
        if (integracao.getPluggyItemId() == null || integracao.getPluggyItemId().isBlank()) {
            throw new IllegalStateException("Pluggy ainda não tem item conectado");
        }
        try {
            List<PluggyGateway.ContaPluggy> contasPluggy = descobrirContas(integracao);
            String apiKey = apiKey(integracao);
            LocalDate ate = LocalDate.now(clock);
            LocalDate de = ate.minusDays(diasRetroativos);
            int inseridas = 0;
            for (PluggyGateway.ContaPluggy conta : contasPluggy) {
                if (!contas.estaAtiva(empresaId, FonteIntegracao.PLUGGY, conta.id())) {
                    continue;
                }
                for (PluggyGateway.TransacaoPluggy transacao : gateway.listarTransacoes(apiKey, conta.id(), de, ate)) {
                    if (ingerirTransacao.ingerir(empresaId, conta, transacao)) {
                        inseridas++;
                    }
                }
                contas.registrarSincronizacao(empresaId, FonteIntegracao.PLUGGY, conta.id());
            }
            integracao.registrarSincronizacao(clock.instant());
            return inseridas;
        } catch (RuntimeException e) {
            integracao.registrarFalha(classificarFalha(e), clock.instant());
            throw e;
        }
    }

    private List<PluggyGateway.ContaPluggy> descobrirContas(IntegracaoPluggy integracao) {
        String apiKey = apiKey(integracao);
        List<PluggyGateway.ContaPluggy> contasPluggy = gateway.listarContas(apiKey, integracao.getPluggyItemId());
        for (PluggyGateway.ContaPluggy conta : contasPluggy) {
            contas.salvarOuAtualizar(
                    integracao.getEmpresaId(),
                    FonteIntegracao.PLUGGY,
                    conta.id(),
                    conta.nome(),
                    conta.bancoCodigo(),
                    null,
                    conta.numero(),
                    null,
                    tipo(conta.tipo()));
        }
        return contasPluggy;
    }

    private String apiKey(IntegracaoPluggy integracao) {
        return gateway.criarApiKey(new PluggyGateway.CredenciaisPluggy(
                cripto.decifrar(integracao.getClientIdCifrado()),
                cripto.decifrar(integracao.getClientSecretCifrado())));
    }

    private TipoContaBancaria tipo(String tipoPluggy) {
        if (tipoPluggy == null) {
            return TipoContaBancaria.OUTRA;
        }
        return switch (tipoPluggy) {
            case "CHECKING_ACCOUNT", "BANK" -> TipoContaBancaria.CORRENTE;
            case "SAVINGS_ACCOUNT" -> TipoContaBancaria.POUPANCA;
            case "CREDIT", "CREDIT_CARD" -> TipoContaBancaria.CARTAO_CREDITO;
            case "PAYMENT_ACCOUNT" -> TipoContaBancaria.PAGAMENTO;
            default -> TipoContaBancaria.OUTRA;
        };
    }

    private String classificarFalha(RuntimeException erro) {
        if (erro instanceof RestClientResponseException resposta
                && (resposta.getStatusCode().value() == 401
                || resposta.getStatusCode().value() == 403)) {
            return "AUTENTICACAO";
        }
        if (erro instanceof RestClientException) {
            return "COMUNICACAO";
        }
        if (erro instanceof IllegalArgumentException) {
            return "DADOS_INVALIDOS";
        }
        return "INTERNO";
    }
}
