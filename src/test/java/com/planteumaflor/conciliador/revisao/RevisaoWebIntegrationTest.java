package com.planteumaflor.conciliador.revisao;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastroRealizado;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        transacoes.salvar(pendente(empresaA.empresaId(), "Descrição exclusiva A"));
        transacoes.salvar(pendente(empresaB.empresaId(), "Descrição exclusiva B"));

        UsuarioPrincipal principal = UsuarioPrincipal.de(
                usuarios.findById(empresaA.usuarioId()).orElseThrow());

        mvc.perform(get("/revisao").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Descrição exclusiva A")))
                .andExpect(content().string(not(containsString("Descrição exclusiva B"))));
    }

    @Test
    void filaExigeAutenticacao() throws Exception {
        mvc.perform(get("/revisao"))
                .andExpect(status().is3xxRedirection());
    }

    private CadastroRealizado cadastrar(String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura"));
    }

    private Transacao pendente(UUID empresaId, String descricao) {
        Transacao transacao = Transacao.ingerida(new DadosTransacao(
                empresaId,
                FonteIntegracao.CORA,
                "ext-" + UUID.randomUUID(),
                "conta-cora",
                "cora",
                LocalDate.of(2026, 6, 24),
                new BigDecimal("25.00"),
                Direcao.CREDITO,
                descricao,
                null,
                null));
        transacao.enviarParaRevisao("nenhuma regra correspondeu");
        return transacao;
    }
}
