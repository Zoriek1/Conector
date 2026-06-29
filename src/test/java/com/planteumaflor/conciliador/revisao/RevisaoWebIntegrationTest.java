package com.planteumaflor.conciliador.revisao;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastroRealizado;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Confianca;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RevisaoWebIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired UsuarioJpaRepository usuarios;
    @Autowired TransacaoRepository transacoes;

    @Test
    void filaMostraSomentePendenciasDaEmpresaAutenticada() throws Exception {
        CadastroRealizado empresaA = cadastrar("revisao-a@example.com");
        CadastroRealizado empresaB = cadastrar("revisao-b@example.com");
        transacoes.salvar(pendente(empresaA.empresaId(), "Descricao exclusiva A"));
        transacoes.salvar(pendente(empresaB.empresaId(), "Descricao exclusiva B"));

        mvc.perform(get("/revisao").with(user(principalDe(empresaA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Descricao exclusiva A")))
                .andExpect(content().string(not(containsString("Descricao exclusiva B"))));
    }

    @Test
    void filaCarregaCssDoApp() throws Exception {
        CadastroRealizado empresa = cadastrar("css@example.com");

        mvc.perform(get("/revisao").with(user(principalDe(empresa))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/tokens.css")))
                .andExpect(content().string(containsString("/css/app.css")));
    }

    @Test
    void filaTemSelecaoEmLoteSemCliqueLinhaALinha() throws Exception {
        CadastroRealizado empresa = cadastrar("lote-ui@example.com");
        transacoes.salvar(pendente(empresa.empresaId(), "item lote ui"));

        mvc.perform(get("/revisao").with(user(principalDe(empresa))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("app-main-wide")))
                .andExpect(content().string(containsString("data-select-page")))
                .andExpect(content().string(containsString("data-select-visible")))
                .andExpect(content().string(containsString("data-select-direction=\"CREDITO\"")))
                .andExpect(content().string(containsString("data-batch-item")))
                .andExpect(content().string(containsString("name=\"size\"")))
                .andExpect(content().string(containsString(">100</option>")));
    }

    @Test
    void buscaLikeAceitaMultiplosTermosEIsolaTenant() throws Exception {
        CadastroRealizado empresaA = cadastrar("busca-a@example.com");
        CadastroRealizado empresaB = cadastrar("busca-b@example.com");
        transacoes.salvar(pendente(novaTransacao(
                empresaA.empresaId(), "Compra Alpha", Direcao.DEBITO,
                "conta-cora", "doc-alpha", "E2E-1")));
        transacoes.salvar(pendente(novaTransacao(
                empresaA.empresaId(), "Segundo item", Direcao.CREDITO,
                "stone destino", "doc-beta", "E2E-BATCH-777")));
        transacoes.salvar(pendente(empresaA.empresaId(), "Nao deve aparecer"));
        transacoes.salvar(pendente(empresaB.empresaId(), "Compra Alpha outra empresa"));

        mvc.perform(get("/revisao")
                        .param("q", "alpha,E2E-BATCH")
                        .with(user(principalDe(empresaA))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Compra Alpha")))
                .andExpect(content().string(containsString("Segundo item")))
                .andExpect(content().string(not(containsString("Nao deve aparecer"))))
                .andExpect(content().string(not(containsString("outra empresa"))));
    }

    @Test
    void filtroPorDirecaoSeparaEntradasESaidas() throws Exception {
        CadastroRealizado empresa = cadastrar("direcao@example.com");
        transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "entrada visivel", Direcao.CREDITO,
                "conta-cora", null, null)));
        transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "saida escondida", Direcao.DEBITO,
                "conta-cora", null, null)));

        mvc.perform(get("/revisao")
                        .param("direcao", "CREDITO")
                        .with(user(principalDe(empresa))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("entrada visivel")))
                .andExpect(content().string(not(containsString("saida escondida"))));
    }

    @Test
    void filaExigeAutenticacao() throws Exception {
        mvc.perform(get("/revisao"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void aprovarMoveItemParaAguardandoApi() throws Exception {
        CadastroRealizado empresa = cadastrar("aprovar@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "aprovar"));

        mvc.perform(post("/revisao/{id}/aprovar", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isOk());

        assertThat(estadoDe(empresa, item.getId()))
                .isEqualTo(EstadoTransacao.AGUARDANDO_ESCRITA_API);
    }

    @Test
    void classificarManualmenteVoltaParaClassificado() throws Exception {
        CadastroRealizado empresa = cadastrar("classificar@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "classificar"));

        mvc.perform(post("/revisao/{id}/classificar", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .param("classe", ClasseTransacao.CREDITO_VENDA.name())
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isOk());

        assertThat(estadoDe(empresa, item.getId())).isEqualTo(EstadoTransacao.CLASSIFICADO);
        assertThat(classeDe(empresa, item.getId())).isEqualTo(ClasseTransacao.CREDITO_VENDA);
    }

    @Test
    void classificarLoteAplicaCategoriaParaSelecionadas() throws Exception {
        CadastroRealizado empresa = cadastrar("lote@example.com");
        Transacao itemA = transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "fornecedor lote A", Direcao.DEBITO,
                "conta-cora", null, null)));
        Transacao itemB = transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "fornecedor lote B", Direcao.DEBITO,
                "conta-cora", null, null)));

        mvc.perform(post("/revisao/lote/classificar")
                        .param("ids", itemA.getId().toString(), itemB.getId().toString())
                        .param("versions", String.valueOf(itemA.getVersion()), String.valueOf(itemB.getVersion()))
                        .param("classe", ClasseTransacao.FLORES_FOLHAGENS_PLANTAS.name())
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fila-container")));

        assertThat(classeDe(empresa, itemA.getId())).isEqualTo(ClasseTransacao.FLORES_FOLHAGENS_PLANTAS);
        assertThat(classeDe(empresa, itemB.getId())).isEqualTo(ClasseTransacao.FLORES_FOLHAGENS_PLANTAS);
        assertThat(estadoDe(empresa, itemA.getId())).isEqualTo(EstadoTransacao.CLASSIFICADO);
        assertThat(estadoDe(empresa, itemB.getId())).isEqualTo(EstadoTransacao.CLASSIFICADO);
    }

    @Test
    void classificarLoteRejeitaCategoriaIncompativelComDirecao() throws Exception {
        CadastroRealizado empresa = cadastrar("lote-incompativel@example.com");
        Transacao item = transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "saida lote", Direcao.DEBITO,
                "conta-cora", null, null)));

        mvc.perform(post("/revisao/lote/classificar")
                        .param("ids", item.getId().toString())
                        .param("versions", String.valueOf(item.getVersion()))
                        .param("classe", ClasseTransacao.CREDITO_VENDA.name())
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isUnprocessableEntity());

        assertThat(classeDe(empresa, item.getId())).isEqualTo(ClasseTransacao.INDEFINIDO);
        assertThat(estadoDe(empresa, item.getId())).isEqualTo(EstadoTransacao.EM_REVISAO);
    }

    @Test
    void classificarLoteComConflitoNaoAlteraNenhumItem() throws Exception {
        CadastroRealizado empresa = cadastrar("lote-conflito@example.com");
        Transacao itemA = transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "lote conflito A", Direcao.DEBITO,
                "conta-cora", null, null)));
        Transacao itemB = transacoes.salvar(pendente(novaTransacao(
                empresa.empresaId(), "lote conflito B", Direcao.DEBITO,
                "conta-cora", null, null)));

        mvc.perform(post("/revisao/lote/classificar")
                        .param("ids", itemA.getId().toString(), itemB.getId().toString())
                        .param("versions", String.valueOf(itemA.getVersion()), String.valueOf(itemB.getVersion() + 99))
                        .param("classe", ClasseTransacao.FLORES_FOLHAGENS_PLANTAS.name())
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isConflict());

        assertThat(classeDe(empresa, itemA.getId())).isEqualTo(ClasseTransacao.INDEFINIDO);
        assertThat(classeDe(empresa, itemB.getId())).isEqualTo(ClasseTransacao.INDEFINIDO);
    }

    @Test
    void classificarLoteEmFalhaResponde422() throws Exception {
        CadastroRealizado empresa = cadastrar("lote-falha@example.com");
        Transacao item = transacoes.salvar(emFalha(empresa.empresaId(), "falha lote"));

        mvc.perform(post("/revisao/lote/classificar")
                        .param("ids", item.getId().toString())
                        .param("versions", String.valueOf(item.getVersion()))
                        .param("classe", ClasseTransacao.FLORES_FOLHAGENS_PLANTAS.name())
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rotearParaOfxMoveItemParaLote() throws Exception {
        CadastroRealizado empresa = cadastrar("ofx@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "ofx"));

        mvc.perform(post("/revisao/{id}/ofx", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isOk());

        assertThat(estadoDe(empresa, item.getId())).isEqualTo(EstadoTransacao.EM_LOTE_OFX);
    }

    @Test
    void matchMantemItemEmRevisao() throws Exception {
        CadastroRealizado empresa = cadastrar("match@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "match"));

        mvc.perform(post("/revisao/{id}/match", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .param("tipo", "CONTA_RECEBER")
                        .param("idExterno", "bling-1")
                        .param("taxa", "1.50")
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isOk());

        assertThat(estadoDe(empresa, item.getId())).isEqualTo(EstadoTransacao.EM_REVISAO);
    }

    @Test
    void retryReprocessaItemEmFalha() throws Exception {
        CadastroRealizado empresa = cadastrar("retry@example.com");
        Transacao item = transacoes.salvar(emFalha(empresa.empresaId(), "retry"));

        mvc.perform(post("/revisao/{id}/retry", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isOk());

        assertThat(estadoDe(empresa, item.getId()))
                .isEqualTo(EstadoTransacao.AGUARDANDO_ESCRITA_API);
    }

    @Test
    void acessoCruzadoEntreEmpresasResponde404() throws Exception {
        CadastroRealizado empresaA = cadastrar("cross-a@example.com");
        CadastroRealizado empresaB = cadastrar("cross-b@example.com");
        Transacao itemB = transacoes.salvar(pendente(empresaB.empresaId(), "cross"));

        mvc.perform(post("/revisao/{id}/aprovar", itemB.getId())
                        .param("version", String.valueOf(itemB.getVersion()))
                        .with(user(principalDe(empresaA))).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void versaoDivergenteResponde409() throws Exception {
        CadastroRealizado empresa = cadastrar("conflito@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "conflito"));

        mvc.perform(post("/revisao/{id}/aprovar", item.getId())
                        .param("version", String.valueOf(item.getVersion() + 99))
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void transicaoInvalidaResponde422() throws Exception {
        CadastroRealizado empresa = cadastrar("invalida@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "invalida"));

        mvc.perform(post("/revisao/{id}/retry", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .with(user(principalDe(empresa))).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void postSemCsrfResponde403() throws Exception {
        CadastroRealizado empresa = cadastrar("semcsrf@example.com");
        Transacao item = transacoes.salvar(pendente(empresa.empresaId(), "semcsrf"));

        mvc.perform(post("/revisao/{id}/aprovar", item.getId())
                        .param("version", String.valueOf(item.getVersion()))
                        .with(user(principalDe(empresa))))
                .andExpect(status().isForbidden());
    }

    @Test
    void fragmentoDaFilaNaoDevolveLayoutCompleto() throws Exception {
        CadastroRealizado empresa = cadastrar("fragmento@example.com");
        transacoes.salvar(pendente(empresa.empresaId(), "fragmento"));

        mvc.perform(get("/revisao/fila").with(user(principalDe(empresa))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fila-container")))
                .andExpect(content().string(not(containsString("app-nav"))));
    }

    @Test
    void filtroPorEstadoListaItensEmFalha() throws Exception {
        CadastroRealizado empresa = cadastrar("falha-filtro@example.com");
        transacoes.salvar(pendente(empresa.empresaId(), "em revisao visivel"));
        transacoes.salvar(emFalha(empresa.empresaId(), "em falha visivel"));

        mvc.perform(get("/revisao").param("estado", "FALHA").with(user(principalDe(empresa))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("em falha visivel")))
                .andExpect(content().string(not(containsString("em revisao visivel"))));
    }

    private EstadoTransacao estadoDe(CadastroRealizado empresa, UUID id) {
        return transacoes.buscarPorId(empresa.empresaId(), id).orElseThrow().getEstado();
    }

    private ClasseTransacao classeDe(CadastroRealizado empresa, UUID id) {
        return transacoes.buscarPorId(empresa.empresaId(), id).orElseThrow().getClasse();
    }

    private UsuarioPrincipal principalDe(CadastroRealizado empresa) {
        return UsuarioPrincipal.de(usuarios.findById(empresa.usuarioId()).orElseThrow());
    }

    private CadastroRealizado cadastrar(String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura"));
    }

    private Transacao pendente(UUID empresaId, String descricao) {
        return pendente(novaTransacao(
                empresaId, descricao, Direcao.CREDITO, "cora", null, null));
    }

    private Transacao pendente(Transacao transacao) {
        transacao.enviarParaRevisao("nenhuma regra correspondeu");
        return transacao;
    }

    private Transacao emFalha(UUID empresaId, String descricao) {
        Transacao transacao = novaTransacao(
                empresaId, descricao, Direcao.DEBITO, "cora", null, null);
        transacao.classificar(
                ClasseTransacao.DEBITO_DESPESA, Confianca.de(new BigDecimal("0.990")), "regra");
        transacao.aprovarParaApi();
        transacao.registrarFalha();
        return transacao;
    }

    private Transacao novaTransacao(
            UUID empresaId,
            String descricao,
            Direcao direcao,
            String conta,
            String documento,
            String e2eId) {
        return Transacao.ingerida(new DadosTransacao(
                empresaId,
                FonteIntegracao.CORA,
                "ext-" + UUID.randomUUID(),
                "conta-cora",
                conta,
                LocalDate.of(2026, 6, 24),
                new BigDecimal("25.00"),
                direcao,
                descricao,
                documento,
                e2eId));
    }
}
