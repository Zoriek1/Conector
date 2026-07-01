package com.planteumaflor.conciliador.pluggy.application;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import com.planteumaflor.conciliador.conta.application.ContaBancariaService;
import com.planteumaflor.conciliador.conta.domain.TipoContaBancaria;
import com.planteumaflor.conciliador.pluggy.domain.IntegracaoPluggy;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PluggyIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(PluggyIntegrationService.class);
    private static final String WEBHOOK_HEADER = "X-Webhook-Secret";
    private static final String WEBHOOK_PATH = "/webhooks/pluggy";

    private final IntegracaoPluggyJpaRepository integracoes;
    private final PluggyGateway gateway;
    private final CriptoService cripto;
    private final ContaBancariaService contas;
    private final IngerirTransacaoPluggyService ingerirTransacao;
    private final Clock clock;
    private final int diasRetroativos;
    private final String webhookSecret;
    private final String webhookUrl;

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
        this.webhookSecret = properties.pluggy().webhookSecret();
        this.webhookUrl = properties.pluggy().publicUrl() + WEBHOOK_PATH;
    }

    @Transactional
    public void salvarCredenciais(UUID empresaId, String clientId, String clientSecret) {
        String apiKey = gateway.criarApiKey(new PluggyGateway.CredenciaisPluggy(clientId, clientSecret));
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
        registrarWebhookBestEffort(empresaId, apiKey);
    }

    /**
     * Registra o webhook na Pluggy usando a apiKey recém-validada da empresa.
     * Best-effort: uma falha aqui não pode impedir o salvamento das credenciais
     * (Backend §3 — validar/salvar credenciais é o caminho crítico).
     */
    private void registrarWebhookBestEffort(UUID empresaId, String apiKey) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("PLUGGY_WEBHOOK_SECRET não configurado — webhook não registrado para empresa {}", empresaId);
            return;
        }
        try {
            gateway.registrarWebhook(apiKey, webhookUrl, "all", Map.of(WEBHOOK_HEADER, webhookSecret));
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar webhook Pluggy para empresa {}: {}", empresaId, e.getMessage());
        }
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

    /**
     * Disparado pelo webhook Pluggy ({@code item/created}/{@code item/updated}).
     * Se o item ainda não tem integração correspondente (ex.: webhook chegou
     * antes do fluxo síncrono de retorno do browser terminar), ignora — é
     * seguro, pois a confirmação via {@link #confirmarItem} cobre esse caso.
     */
    @Async
    @Transactional
    public void processarEventoItem(String pluggyItemId) {
        integracoes.findByPluggyItemId(pluggyItemId).ifPresentOrElse(
                this::descobrirContas,
                () -> log.info("webhook Pluggy: item {} sem integração correspondente, ignorando", pluggyItemId));
    }

    /**
     * Disparado pelo webhook Pluggy ({@code transactions/created}/{@code transactions/updated}).
     * Reaproveita {@link #sincronizar} em vez de processar o payload do evento
     * transação a transação — a app é de baixo volume e a sincronização já é
     * idempotente.
     */
    @Async
    @Transactional
    public void processarEventoTransacoes(String pluggyItemId) {
        integracoes.findByPluggyItemId(pluggyItemId).ifPresentOrElse(
                integracao -> sincronizar(integracao.getEmpresaId()),
                () -> log.info("webhook Pluggy: item {} sem integração correspondente, ignorando", pluggyItemId));
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
