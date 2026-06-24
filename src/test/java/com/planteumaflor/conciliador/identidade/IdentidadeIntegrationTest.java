package com.planteumaflor.conciliador.identidade;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.empresa.persistence.EmpresaJpaRepository;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastroRealizado;
import com.planteumaflor.conciliador.identidade.application.EmailJaCadastradoException;
import com.planteumaflor.conciliador.identidade.domain.Usuario;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Testes do passo 2 (identidade) contra PostgreSQL real.
 *
 * {@code @Transactional} faz cada teste dar rollback ao final, isolando os
 * dados entre os casos.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentidadeIntegrationTest {

    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired EmpresaJpaRepository empresas;
    @Autowired UsuarioJpaRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;

    @Test
    void cadastroCriaEmpresaEUsuarioComSenhaHasheada() {
        CadastroRealizado r = cadastrar.executar(new CadastrarEmpresaCommand(
                "Plante Uma Flor", "12345678000199", "Ana", "Ana@Example.com ", "senha-supersegura"));

        assertThat(empresas.findById(r.empresaId())).isPresent();
        Usuario u = usuarios.findById(r.usuarioId()).orElseThrow();
        assertThat(u.getEmpresaId()).isEqualTo(r.empresaId());
        assertThat(u.getEmail()).isEqualTo("ana@example.com");            // normalizado
        assertThat(u.getSenhaHash()).isNotEqualTo("senha-supersegura");   // nunca em texto
        assertThat(encoder.matches("senha-supersegura", u.getSenhaHash())).isTrue();
    }

    @Test
    void emailDuplicadoNaoCriaSegundaEmpresa() {
        cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa A", null, "Ana", "dup@example.com", "senha-supersegura"));
        long empresasAntes = empresas.count();

        assertThatThrownBy(() -> cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa B", null, "Bia", "DUP@example.com", "outra-senha8")))
                .isInstanceOf(EmailJaCadastradoException.class);

        assertThat(empresas.count()).isEqualTo(empresasAntes);            // não criou a 2ª
    }

    @Test
    void loginValidoSemPluggyVaiParaOnboarding() throws Exception {
        // Empresa recém-criada não tem Pluggy ativo -> onboarding (tela 01).
        cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", "login@example.com", "senha-supersegura"));

        mvc.perform(formLogin("/entrar").user("login@example.com").password("senha-supersegura"))
                .andExpect(authenticated().withUsername("login@example.com"))
                .andExpect(redirectedUrl("/onboarding"));
    }

    @Test
    void loginInvalidoNaoAutentica() throws Exception {
        cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", "real@example.com", "senha-supersegura"));

        mvc.perform(formLogin("/entrar").user("real@example.com").password("senha-errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/entrar?erro"));
    }

    @Test
    void duasEmpresasTemTenantsDistintos() {
        CadastroRealizado a = cadastrar.executar(new CadastrarEmpresaCommand(
                "A", null, "Ana", "a@example.com", "senha-supersegura"));
        CadastroRealizado b = cadastrar.executar(new CadastrarEmpresaCommand(
                "B", null, "Bia", "b@example.com", "senha-supersegura"));

        assertThat(a.empresaId()).isNotEqualTo(b.empresaId());
        Usuario ua = usuarios.findById(a.usuarioId()).orElseThrow();
        Usuario ub = usuarios.findById(b.usuarioId()).orElseThrow();
        assertThat(ua.getEmpresaId()).isEqualTo(a.empresaId());
        assertThat(ub.getEmpresaId()).isEqualTo(b.empresaId());
        assertThat(ua.getEmpresaId()).isNotEqualTo(ub.getEmpresaId());
    }
}
