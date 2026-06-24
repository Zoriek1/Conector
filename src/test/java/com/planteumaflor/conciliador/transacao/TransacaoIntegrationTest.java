package com.planteumaflor.conciliador.transacao;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Confianca;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransacaoIntegrationTest {

    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired TransacaoRepository transacoes;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;

    private TransactionTemplate tx;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);
        limparBanco();
    }

    @AfterEach
    void limpar() {
        limparBanco();
    }

    @Test
    void persisteEConsultaSomenteDentroDoTenant() {
        UUID empresaA = cadastrarEmpresa("transacao-a@example.com");
        UUID empresaB = cadastrarEmpresa("transacao-b@example.com");

        Transacao salva = salvar(novaTransacao(empresaA, "pluggy-1", "10.00"));

        assertThat(transacoes.buscarPorId(empresaA, salva.getId())).isPresent();
        assertThat(transacoes.buscarPorId(empresaB, salva.getId())).isEmpty();
        assertThat(transacoes.existePorOrigem(empresaA, FonteIntegracao.PLUGGY, "pluggy-1")).isTrue();
        assertThat(transacoes.existePorOrigem(empresaB, FonteIntegracao.PLUGGY, "pluggy-1")).isFalse();
    }

    @Test
    void idPluggyEhUnicoPorEmpresaENaoGlobal() {
        UUID empresaA = cadastrarEmpresa("unica-a@example.com");
        UUID empresaB = cadastrarEmpresa("unica-b@example.com");

        salvar(novaTransacao(empresaA, "mesmo-id", "10.00"));
        salvar(novaTransacao(empresaB, "mesmo-id", "20.00"));

        assertThatThrownBy(() -> salvar(novaTransacao(empresaA, "mesmo-id", "30.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optimisticLockImpedeDuasAtualizacoesConcorrentes() {
        UUID empresa = cadastrarEmpresa("lock@example.com");
        Transacao salva = salvar(novaTransacao(empresa, "lock-id", "40.00"));

        EntityManager primeiroEm = entityManagerFactory.createEntityManager();
        EntityManager segundoEm = entityManagerFactory.createEntityManager();
        try {
            primeiroEm.getTransaction().begin();
            segundoEm.getTransaction().begin();

            Transacao primeiraCopia = primeiroEm.find(Transacao.class, salva.getId());
            Transacao segundaCopia = segundoEm.find(Transacao.class, salva.getId());
            primeiraCopia.classificar(
                    ClasseTransacao.CREDITO_VENDA,
                    Confianca.de(new BigDecimal("0.950")),
                    "primeira classificação");
            segundaCopia.classificar(
                    ClasseTransacao.DEBITO_DESPESA,
                    Confianca.de(new BigDecimal("0.940")),
                    "segunda classificação");

            primeiroEm.getTransaction().commit();

            assertThatThrownBy(segundoEm.getTransaction()::commit)
                    .hasCauseInstanceOf(OptimisticLockException.class);
        } finally {
            if (primeiroEm.getTransaction().isActive()) {
                primeiroEm.getTransaction().rollback();
            }
            if (segundoEm.getTransaction().isActive()) {
                segundoEm.getTransaction().rollback();
            }
            primeiroEm.close();
            segundoEm.close();
        }
    }

    @Test
    void bancoTambemProtegeValorEConfianca() {
        UUID empresa = cadastrarEmpresa("constraint@example.com");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transacao (
                    empresa_id, fonte, id_transacao_externa, id_conta_externa, conta_local,
                    data, valor_liquido, direcao, classe, confianca, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                empresa, "PLUGGY", "invalida", "conta", "cora", LocalDate.now(),
                BigDecimal.ZERO, "CREDITO", "INDEFINIDO", new BigDecimal("1.100"), "INGERIDO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID cadastrarEmpresa(String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura")).empresaId();
    }

    private Transacao salvar(Transacao transacao) {
        return tx.execute(status -> transacoes.salvar(transacao));
    }

    private Transacao novaTransacao(UUID empresaId, String idPluggy, String valor) {
        return Transacao.ingerida(new DadosTransacao(
                empresaId,
                FonteIntegracao.PLUGGY,
                idPluggy,
                "conta-" + idPluggy,
                "cora",
                LocalDate.of(2026, 6, 23),
                new BigDecimal(valor),
                Direcao.CREDITO,
                "Recebimento",
                null,
                null));
    }

    private void limparBanco() {
        jdbc.update("DELETE FROM transacao");
        jdbc.update("DELETE FROM integracao_pluggy");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM empresa");
    }
}
