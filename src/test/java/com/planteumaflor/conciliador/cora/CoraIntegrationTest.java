package com.planteumaflor.conciliador.cora;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.config.CriptoService;
import com.planteumaflor.conciliador.cora.application.CadastrarCredencialCora;
import com.planteumaflor.conciliador.cora.application.CoraGateway;
import com.planteumaflor.conciliador.cora.application.SincronizarExtratoCora;
import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import com.planteumaflor.conciliador.cora.domain.StatusIntegracaoCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do bounded context Cora (cadastro de credencial cifrada, sincronização
 * idempotente, isolamento por tenant), usando um {@link FakeCoraGatewayConfig}
 * em vez de mTLS/HTTP real.
 */
@Import({TestcontainersConfiguration.class, FakeCoraGatewayConfig.class})
@SpringBootTest(properties = "conciliador.cripto.chave=grmb3VJBqcpsLe/K3GPv9olyug4aQqstlzhXptcGMss=")
class CoraIntegrationTest {

    @Autowired CadastrarEmpresaEUsuario cadastrarEmpresa;
    @Autowired CadastrarCredencialCora cadastrarCredencial;
    @Autowired SincronizarExtratoCora sincronizarExtrato;
    @Autowired IntegracaoCoraJpaRepository integracoes;
    @Autowired TransacaoRepository transacoes;
    @Autowired CriptoService cripto;
    @Autowired FakeCoraGatewayConfig.FakeCoraGateway gateway;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        gateway.definirLancamentos(java.util.List.of());
        limparBanco();
    }

    @AfterEach
    void limpar() {
        limparBanco();
    }

    @Test
    void cadastrarCredencialValidaECifraAntesDePersistir() {
        UUID empresaId = cadastrarEmpresa("cora-a@example.com");

        cadastrarCredencial.cadastrar(empresaId, "client-1", "cert-pem", "chave-pem");

        IntegracaoCora integracao = integracoes.findByEmpresaId(empresaId).orElseThrow();
        assertThat(integracao.getStatus()).isEqualTo(StatusIntegracaoCora.ATIVA);
        assertThat(integracao.getClientIdCifrado()).isNotEqualTo("client-1");
        assertThat(cripto.decifrar(integracao.getClientIdCifrado())).isEqualTo("client-1");
        assertThat(cripto.decifrar(integracao.getCertificadoCifrado())).isEqualTo("cert-pem");
        assertThat(cripto.decifrar(integracao.getChavePrivadaCifrada())).isEqualTo("chave-pem");
    }

    @Test
    void sincronizarIngereEClassificaLancamentosNovos() {
        UUID empresaId = cadastrarEmpresa("cora-b@example.com");
        cadastrarCredencial.cadastrar(empresaId, "client-1", "cert-pem", "chave-pem");
        gateway.definirLancamentos(java.util.List.of(
                lancamento("ext-1", "100.00")));

        sincronizarExtrato.sincronizar(empresaId);

        assertThat(transacoes.existePorOrigem(empresaId, FonteIntegracao.CORA, "ext-1")).isTrue();
        assertThat(integracoes.findByEmpresaId(empresaId).orElseThrow().getUltimaSincronizacao()).isNotNull();
    }

    @Test
    void sincronizarDuasVezesNaoDuplicaTransacoes() {
        UUID empresaId = cadastrarEmpresa("cora-c@example.com");
        cadastrarCredencial.cadastrar(empresaId, "client-1", "cert-pem", "chave-pem");
        gateway.definirLancamentos(java.util.List.of(
                lancamento("ext-2", "50.00")));

        sincronizarExtrato.sincronizar(empresaId);
        sincronizarExtrato.sincronizar(empresaId);

        long total = jdbc.queryForObject(
                "SELECT count(*) FROM transacao WHERE empresa_id = ? AND id_transacao_externa = ?",
                Long.class, empresaId, "ext-2");
        assertThat(total).isEqualTo(1);
    }

    @Test
    void integracaoEhIsoladaPorEmpresa() {
        UUID empresaA = cadastrarEmpresa("cora-d@example.com");
        UUID empresaB = cadastrarEmpresa("cora-e@example.com");

        cadastrarCredencial.cadastrar(empresaA, "client-a", "cert-a", "chave-a");

        assertThat(integracoes.findByEmpresaId(empresaA)).isPresent();
        assertThat(integracoes.findByEmpresaId(empresaB)).isEmpty();
        assertThatSincronizarFalhaSemIntegracao(empresaB);
    }

    @Test
    void consultaTransacoesPaginadaSoTraDaEmpresa() {
        UUID empresaA = cadastrarEmpresa("cora-f@example.com");
        UUID empresaB = cadastrarEmpresa("cora-g@example.com");
        cadastrarCredencial.cadastrar(empresaA, "client-1", "cert-pem", "chave-pem");
        gateway.definirLancamentos(java.util.List.of(lancamento("ext-3", "30.00")));
        sincronizarExtrato.sincronizar(empresaA);

        assertThat(transacoes.listarPorEmpresa(empresaA, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(transacoes.listarPorEmpresa(empresaB, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(0);
    }

    private void assertThatSincronizarFalhaSemIntegracao(UUID empresaId) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sincronizarExtrato.sincronizar(empresaId))
                .isInstanceOf(IllegalStateException.class);
    }

    private CoraGateway.LancamentoExtrato lancamento(String idExterno, String valor) {
        return new CoraGateway.LancamentoExtrato(
                idExterno, "conta-cora-1", LocalDate.of(2026, 6, 20),
                new BigDecimal(valor), "PIX recebido de cliente", null, null);
    }

    private UUID cadastrarEmpresa(String email) {
        return cadastrarEmpresa.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura")).empresaId();
    }

    private void limparBanco() {
        jdbc.update("DELETE FROM transacao");
        jdbc.update("DELETE FROM integracao_cora");
        jdbc.update("DELETE FROM integracao_pluggy");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM empresa");
    }
}
