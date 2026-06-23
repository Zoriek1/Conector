package com.planteumaflor.conciliador;

import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de fumaça do esqueleto, contra PostgreSQL real via Testcontainers.
 *
 * Cobre os critérios do passo 1 (Setup-SpringBoot §12):
 * - o contexto sobe, o Flyway roda e o ddl-auto=validate aceita o schema;
 * - o repositório JPA consulta o banco;
 * - /actuator/health é liberado sem autenticação;
 * - uma rota protegida redireciona para o login quando não autenticado.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ConciliadorApplicationTests {

	@Autowired
	UsuarioJpaRepository usuarios;

	@Autowired
	MockMvc mvc;

	@Test
	void contextSobe_eSchemaFlywayValida() {
		// Chegar aqui já prova: contexto carregado, Flyway aplicado, validate OK.
		assertThat(usuarios.count()).isZero();
	}

	@Test
	void healthLiberadoSemAutenticacao() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void rotaProtegidaRedirecionaParaLogin() throws Exception {
		mvc.perform(get("/inicio")).andExpect(status().is3xxRedirection());
	}

}
