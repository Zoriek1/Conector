package com.planteumaflor.conciliador.identidade;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilWebIntegrationTest {

    private static final String SENHA = "senha-supersegura";
    private static final String NOVA_SENHA = "outra-senha-mais-forte";

    @Autowired MockMvc mvc;
    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired UsuarioJpaRepository usuarios;
    @Autowired PasswordEncoder encoder;

    @Test
    void perfilMostraDadosDaEmpresaECnpjMascarado() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(
                "Floricultura", "12345678000190", "perfil@example.com"));

        mvc.perform(get("/perfil").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Floricultura")))
                .andExpect(content().string(containsString("perfil@example.com")))
                .andExpect(content().string(containsString("0190")))
                // CNPJ completo nunca aparece em claro
                .andExpect(content().string(not(containsString("12345678000190"))));
    }

    @Test
    void alterarSenhaComSucessoTrocaHashERedireciona() throws Exception {
        CadastroRealizado cadastro = cadastrar(null, "troca@example.com");
        UsuarioPrincipal principal = principal(cadastro);

        mvc.perform(post("/perfil/senha").with(user(principal)).with(csrf())
                        .param("senhaAtual", SENHA)
                        .param("novaSenha", NOVA_SENHA)
                        .param("confirmacao", NOVA_SENHA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil?senha"));

        var usuario = usuarios.findById(cadastro.usuarioId()).orElseThrow();
        assertThat(encoder.matches(NOVA_SENHA, usuario.getSenhaHash())).isTrue();
        assertThat(usuario.getSenhaAlteradaEm()).isNotNull();
    }

    @Test
    void senhaAtualIncorretaRetorna422SemEcoarNovaSenha() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(null, "errada@example.com"));

        mvc.perform(post("/perfil/senha").with(user(principal)).with(csrf())
                        .param("senhaAtual", "senha-incorreta")
                        .param("novaSenha", NOVA_SENHA)
                        .param("confirmacao", NOVA_SENHA))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(containsString("A senha atual está incorreta.")))
                .andExpect(content().string(not(containsString(NOVA_SENHA))));
    }

    @Test
    void novaSenhaCurtaRetorna422() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(null, "curta@example.com"));

        mvc.perform(post("/perfil/senha").with(user(principal)).with(csrf())
                        .param("senhaAtual", SENHA)
                        .param("novaSenha", "curta")
                        .param("confirmacao", "curta"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void confirmacaoDivergenteRetorna422() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(null, "divergente@example.com"));

        mvc.perform(post("/perfil/senha").with(user(principal)).with(csrf())
                        .param("senhaAtual", SENHA)
                        .param("novaSenha", NOVA_SENHA)
                        .param("confirmacao", "diferente-ainda-assim-longa"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string(containsString("As senhas não conferem.")));
    }

    @Test
    void atualizarNomeResponsavelPersisteERedireciona() throws Exception {
        CadastroRealizado cadastro = cadastrar(null, "nome@example.com");
        UsuarioPrincipal principal = principal(cadastro);

        mvc.perform(post("/perfil/dados").with(user(principal)).with(csrf())
                        .param("nomeResponsavel", "Bruno Atualizado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil?dados"));

        assertThat(usuarios.findById(cadastro.usuarioId()).orElseThrow().getNome())
                .isEqualTo("Bruno Atualizado");
    }

    @Test
    void nomeResponsavelVazioRetorna422() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(null, "vazio@example.com"));

        mvc.perform(post("/perfil/dados").with(user(principal)).with(csrf())
                        .param("nomeResponsavel", "  "))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void encerrarOutrasSessoesRedireciona() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(null, "sessoes@example.com"));

        mvc.perform(post("/perfil/sessoes/encerrar-outras").with(user(principal)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil?sessoes"));
    }

    @Test
    void mutacoesExigemCsrf() throws Exception {
        UsuarioPrincipal principal = principal(cadastrar(null, "csrf@example.com"));
        MockHttpSession sessao = new MockHttpSession();

        mvc.perform(post("/perfil/senha").session(sessao).with(user(principal))
                        .with(request -> {
                            request.setRequestedSessionIdValid(true);
                            return request;
                        })
                        .param("senhaAtual", SENHA)
                        .param("novaSenha", NOVA_SENHA)
                        .param("confirmacao", NOVA_SENHA))
                .andExpect(status().isForbidden());
    }

    private CadastroRealizado cadastrar(String cnpj, String email) {
        return cadastrar("Empresa", cnpj, email);
    }

    private CadastroRealizado cadastrar(String nomeEmpresa, String cnpj, String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(nomeEmpresa, cnpj, "Ana", email, SENHA));
    }

    private UsuarioPrincipal principal(CadastroRealizado cadastro) {
        return UsuarioPrincipal.de(usuarios.findById(cadastro.usuarioId()).orElseThrow());
    }
}
