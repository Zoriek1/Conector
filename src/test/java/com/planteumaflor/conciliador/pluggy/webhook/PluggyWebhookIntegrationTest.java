package com.planteumaflor.conciliador.pluggy.webhook;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.pluggy.FakePluggyGatewayConfig;
import com.planteumaflor.conciliador.pluggy.application.PluggyGateway;
import com.planteumaflor.conciliador.pluggy.application.PluggyIntegrationService;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do endpoint receptor de webhook Pluggy (checklist "Solicitar Acesso
 * à Produção" §3), usando {@link FakePluggyGatewayConfig} em vez de HTTP real.
 *
 * Sem {@code @Transactional} de propósito: o processamento do webhook roda em
 * outra thread ({@code @Async}), então o setup precisa estar commitado antes
 * da chamada HTTP — mesmo padrão de limpeza manual via JDBC do
 * {@code CoraIntegrationTest}.
 */
@Import({TestcontainersConfiguration.class, FakePluggyGatewayConfig.class})
@SpringBootTest(properties = {
        "conciliador.cripto.chave=grmb3VJBqcpsLe/K3GPv9olyug4aQqstlzhXptcGMss=",
        "conciliador.pluggy.webhook-secret=segredo-de-teste"
})
@AutoConfigureMockMvc
class PluggyWebhookIntegrationTest {

    private static final String HEADER = "X-Webhook-Secret";
    private static final String SEGREDO = "segredo-de-teste";

    @Autowired MockMvc mvc;
    @Autowired CadastrarEmpresaEUsuario cadastrarEmpresa;
    @Autowired PluggyIntegrationService pluggy;
    @Autowired TransacaoRepository transacoes;
    @Autowired FakePluggyGatewayConfig.FakePluggyGateway gateway;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        gateway.definirContas(List.of());
        gateway.definirTransacoes(List.of());
        gateway.definirFalhaRegistrarWebhook(null);
        limparBanco();
    }

    @AfterEach
    void limpar() {
        limparBanco();
    }

    @Test
    void salvarCredenciaisRegistraWebhookComEventoAllESegredoConfigurado() {
        UUID empresaId = cadastrarEmpresa("webhook-registro@example.com");

        pluggy.salvarCredenciais(empresaId, "client-1", "secret-1");

        List<FakePluggyGatewayConfig.FakePluggyGateway.WebhookRegistrado> registrados = gateway.webhooksRegistrados();
        assertThat(registrados).hasSize(1);
        assertThat(registrados.get(0).evento()).isEqualTo("all");
        assertThat(registrados.get(0).url()).endsWith("/webhooks/pluggy");
        assertThat(registrados.get(0).headers()).containsEntry(HEADER, SEGREDO);
    }

    @Test
    void falhaAoRegistrarWebhookNaoImpedeSalvarCredenciais() {
        UUID empresaId = cadastrarEmpresa("webhook-falha@example.com");
        gateway.definirFalhaRegistrarWebhook(new RuntimeException("indisponível"));

        pluggy.salvarCredenciais(empresaId, "client-1", "secret-1");

        assertThat(gateway.webhooksRegistrados()).isEmpty();
    }

    @Test
    void webhookComHeaderValidoSincronizaTransacoesDoItem() throws Exception {
        UUID empresaId = cadastrarEmpresa("webhook-sync@example.com");
        pluggy.salvarCredenciais(empresaId, "client-1", "secret-1");
        gateway.definirContas(List.of(conta("conta-1", "item-1")));
        pluggy.confirmarItem(empresaId, "item-1");
        gateway.definirTransacoes(List.of(transacao("tx-1")));

        mvc.perform(post("/webhooks/pluggy")
                        .header(HEADER, SEGREDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"transactions/updated","eventId":"e1","itemId":"item-1"}"""))
                .andExpect(status().isOk());

        aguardarAte(() -> transacoes.existePorOrigem(empresaId, FonteIntegracao.PLUGGY, "tx-1"));
    }

    @Test
    void webhookSemHeaderERejeitadoENaoProcessaNada() throws Exception {
        UUID empresaId = cadastrarEmpresa("webhook-sem-header@example.com");
        pluggy.salvarCredenciais(empresaId, "client-1", "secret-1");
        gateway.definirContas(List.of(conta("conta-2", "item-2")));
        pluggy.confirmarItem(empresaId, "item-2");
        gateway.definirTransacoes(List.of(transacao("tx-2")));

        mvc.perform(post("/webhooks/pluggy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"transactions/updated","eventId":"e2","itemId":"item-2"}"""))
                .andExpect(status().isUnauthorized());

        assertThat(transacoes.existePorOrigem(empresaId, FonteIntegracao.PLUGGY, "tx-2")).isFalse();
    }

    @Test
    void webhookComHeaderErradoERejeitado() throws Exception {
        mvc.perform(post("/webhooks/pluggy")
                        .header(HEADER, "valor-errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"transactions/updated","eventId":"e3","itemId":"item-3"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookComItemIdDesconhecidoRespondeOkSemProcessar() throws Exception {
        mvc.perform(post("/webhooks/pluggy")
                        .header(HEADER, SEGREDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"transactions/updated","eventId":"e4","itemId":"item-inexistente"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void webhookDeTransacaoDeletadaRespondeOkSemAcao() throws Exception {
        mvc.perform(post("/webhooks/pluggy")
                        .header(HEADER, SEGREDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"transactions/deleted","eventId":"e5","itemId":"item-5"}"""))
                .andExpect(status().isOk());
    }

    private void aguardarAte(BooleanSupplier condicao) throws InterruptedException {
        long limite = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < limite) {
            if (condicao.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        fail("condição não atingida a tempo");
    }

    private PluggyGateway.ContaPluggy conta(String id, String itemId) {
        return new PluggyGateway.ContaPluggy(id, itemId, "Conta Fake", "1234", "CHECKING_ACCOUNT", "001");
    }

    private PluggyGateway.TransacaoPluggy transacao(String idExterno) {
        return new PluggyGateway.TransacaoPluggy(
                idExterno, "conta-1", LocalDate.of(2026, 6, 20),
                new BigDecimal("100.00"), "CREDIT", "PIX recebido", null, null, null);
    }

    private UUID cadastrarEmpresa(String email) {
        return cadastrarEmpresa.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura")).empresaId();
    }

    private void limparBanco() {
        jdbc.update("DELETE FROM transacao");
        jdbc.update("DELETE FROM conta_bancaria");
        jdbc.update("DELETE FROM integracao_cora");
        jdbc.update("DELETE FROM integracao_pluggy");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM empresa");
    }
}
