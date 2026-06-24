package com.planteumaflor.conciliador.identidade;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.empresa.persistence.EmpresaJpaRepository;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastroRealizado;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentidadeWebIntegrationTest {

    private static final String SENHA = "senha-supersegura";

    @Autowired MockMvc mvc;
    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired UsuarioJpaRepository usuarios;
    @Autowired EmpresaJpaRepository empresas;
    @Autowired PasswordEncoder encoder;

    @Test
    void formulariosPublicosSaoRenderizados() throws Exception {
        mvc.perform(get("/entrar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("E-mail")));

        mvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Criar conta")));
    }

    @Test
    void usuarioAutenticadoNaoVeFormularioDeLogin() throws Exception {
        UsuarioPrincipal principal = principal(cadastrarEmpresa("autenticado@example.com"));

        mvc.perform(get("/entrar").with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding"));
    }

    @Test
    void loginRetomaSavedRequestInternoERenovaSessao() throws Exception {
        cadastrarEmpresa("saved@example.com");

        MvcResult acessoProtegido = mvc.perform(get("/inicio"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) acessoProtegido.getRequest().getSession(false);
        assertThat(sessao).isNotNull();
        String idAnterior = sessao.getId();

        MvcResult login = mvc.perform(post("/entrar")
                        .session(sessao)
                        .with(csrf())
                        .param("username", "saved@example.com")
                        .param("password", SENHA))
                .andExpect(authenticated().withUsername("saved@example.com"))
                .andExpect(redirectedUrl("/inicio"))
                .andReturn();

        assertThat(login.getRequest().getSession(false).getId()).isNotEqualTo(idAnterior);
    }

    @Test
    void parametroDeRedirectExternoEhIgnorado() throws Exception {
        cadastrarEmpresa("redirect@example.com");

        mvc.perform(post("/entrar")
                        .queryParam("redirect", "https://exemplo-malicioso.test")
                        .with(csrf())
                        .param("username", "redirect@example.com")
                        .param("password", SENHA))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/onboarding"));
    }

    @Test
    void falhasDeLoginTemRespostaGenerica() throws Exception {
        cadastrarEmpresa("real@example.com");

        mvc.perform(post("/entrar").with(csrf())
                        .param("username", "real@example.com")
                        .param("password", "senha-incorreta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar?erro"));

        mvc.perform(post("/entrar").with(csrf())
                        .param("username", "inexistente@example.com")
                        .param("password", "senha-incorreta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar?erro"));
    }

    @Test
    void sessaoExpiradaTemMensagemEIdInvalidoRedireciona() throws Exception {
        mvc.perform(get("/entrar").queryParam("expirada", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sua sessão expirou")));

        mvc.perform(get("/inicio").with(request -> {
                    request.setRequestedSessionId("sessao-inexistente");
                    request.setRequestedSessionIdValid(false);
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar?expirada"));
    }

    @Test
    void logoutExigeCsrf() throws Exception {
        UsuarioPrincipal principal = principal(cadastrarEmpresa("logout@example.com"));
        MockHttpSession sessao = new MockHttpSession();

        mvc.perform(post("/sair")
                        .session(sessao)
                        .with(user(principal))
                        .with(request -> {
                            request.setRequestedSessionIdValid(true);
                            return request;
                        }))
                .andExpect(status().isForbidden());

        mvc.perform(post("/sair").session(sessao).with(user(principal)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar?saiu"));
    }

    @Test
    void cadastroValidoAutenticaERedirecionaParaOnboarding() throws Exception {
        MvcResult resultado = mvc.perform(cadastroPost("nova@example.com").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding"))
                .andExpect(authenticated().withUsername("nova@example.com"))
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/onboarding").session(sessao))
                .andExpect(status().isOk());

        var usuario = usuarios.findByEmail("nova@example.com").orElseThrow();
        assertThat(encoder.matches(SENHA, usuario.getSenhaHash())).isTrue();
    }

    @Test
    void cadastroInvalidoRetorna422ESenhasVazias() throws Exception {
        mvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("nomeEmpresa", "")
                        .param("nomeResponsavel", "Ana")
                        .param("email", "ana@example.com")
                        .param("senha", SENHA)
                        .param("confirmarSenha", "senha-diferente"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(not(containsString("value=\"" + SENHA + "\""))))
                .andExpect(content().string(not(containsString("value=\"senha-diferente\""))));
    }

    @Test
    void emailDuplicadoRetorna422SemCriarOutraEmpresa() throws Exception {
        cadastrarEmpresa("duplicado@example.com");
        long empresasAntes = empresas.count();

        mvc.perform(cadastroPost("DUPLICADO@example.com").with(csrf()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(containsString(
                        "Não foi possível concluir o cadastro com esse e-mail.")))
                .andExpect(content().string(not(containsString("value=\"" + SENHA + "\""))));

        assertThat(empresas.count()).isEqualTo(empresasAntes);
    }

    @Test
    void cadastroIgnoraTenantEPapelEnviadosPeloNavegador() throws Exception {
        UUID empresaForjada = UUID.randomUUID();

        mvc.perform(cadastroPost("tenant@example.com")
                        .with(csrf())
                        .param("empresaId", empresaForjada.toString())
                        .param("papel", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated());

        var usuario = usuarios.findByEmail("tenant@example.com").orElseThrow();
        assertThat(usuario.getEmpresaId()).isNotEqualTo(empresaForjada);
    }

    @Test
    void cadastroExigeCsrf() throws Exception {
        mvc.perform(cadastroPost("csrf@example.com")
                        .session(new MockHttpSession())
                        .with(request -> {
                            request.setRequestedSessionIdValid(true);
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    private CadastroRealizado cadastrarEmpresa(String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, SENHA));
    }

    private UsuarioPrincipal principal(CadastroRealizado cadastro) {
        return UsuarioPrincipal.de(usuarios.findById(cadastro.usuarioId()).orElseThrow());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder cadastroPost(String email) {
        return post("/cadastro")
                .param("nomeEmpresa", "Empresa")
                .param("cnpj", "")
                .param("nomeResponsavel", "Ana")
                .param("email", email)
                .param("senha", SENHA)
                .param("confirmarSenha", SENHA);
    }
}
