package com.planteumaflor.conciliador.onboarding;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastroRealizado;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.onboarding.application.ConsultarOnboarding;
import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;
import com.planteumaflor.conciliador.pluggy.application.ConectarPluggy;
import com.planteumaflor.conciliador.pluggy.domain.StatusIntegracao;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do passo 3 (onboarding + fake Pluggy) contra PostgreSQL real.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OnboardingIntegrationTest {

    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired UsuarioJpaRepository usuarios;
    @Autowired ConsultarOnboarding onboarding;
    @Autowired ConectarPluggy conectarPluggy;
    @Autowired IntegracaoPluggyJpaRepository integracoes;
    @Autowired MockMvc mvc;

    private CadastroRealizado cadastrarEmpresa(String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura"));
    }

    @Test
    void empresaNovaComecaComPluggyPendente() {
        UUID empresaId = cadastrarEmpresa("a@example.com").empresaId();
        assertThat(onboarding.etapaAtual(empresaId)).isEqualTo(EtapaOnboarding.PLUGGY_PENDENTE);
    }

    @Test
    void conectarPersisteIntegracaoEAvancaParaConcluido() {
        UUID empresaId = cadastrarEmpresa("b@example.com").empresaId();

        conectarPluggy.conectar(empresaId);

        assertThat(integracoes.existsByEmpresaIdAndStatus(empresaId, StatusIntegracao.ATIVA)).isTrue();
        assertThat(onboarding.etapaAtual(empresaId)).isEqualTo(EtapaOnboarding.CONCLUIDO);
    }

    @Test
    void conectarDuasVezesNaoDuplica() {
        UUID empresaId = cadastrarEmpresa("c@example.com").empresaId();

        conectarPluggy.conectar(empresaId);
        conectarPluggy.conectar(empresaId);

        long doTenant = integracoes.findAll().stream()
                .filter(i -> i.getEmpresaId().equals(empresaId))
                .count();
        assertThat(doTenant).isEqualTo(1);
    }

    @Test
    void postConectarAutenticadoRedirecionaParaOnboarding() throws Exception {
        CadastroRealizado r = cadastrarEmpresa("d@example.com");
        UsuarioPrincipal principal = UsuarioPrincipal.de(usuarios.findById(r.usuarioId()).orElseThrow());

        mvc.perform(post("/integracoes/pluggy/conectar").with(csrf()).with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding"));

        assertThat(onboarding.etapaAtual(r.empresaId())).isEqualTo(EtapaOnboarding.CONCLUIDO);
    }

    @Test
    void loginComPluggyAtivoVaiParaInicio() throws Exception {
        UUID empresaId = cadastrarEmpresa("e@example.com").empresaId();
        conectarPluggy.conectar(empresaId);

        mvc.perform(formLogin("/entrar").user("e@example.com").password("senha-supersegura"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/inicio"));
    }
}
