package com.planteumaflor.conciliador.transacao;

import com.planteumaflor.conciliador.TestcontainersConfiguration;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.transacao.application.TransferenciasInternas;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Confianca;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.OrigemTransferencia;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TransferenciaInternaServiceIntegrationTest {

    private static final LocalDate DIA = LocalDate.of(2026, 6, 23);

    @Autowired CadastrarEmpresaEUsuario cadastrar;
    @Autowired TransacaoRepository transacoes;
    @Autowired TransferenciasInternas transferencias;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    private TransactionTemplate tx;
    private final AtomicInteger sequencia = new AtomicInteger();

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
    void pareiaParInequivocoEOcultaDasListas() {
        UUID empresa = cadastrarEmpresa("par-unico@example.com");
        Transacao credito = persistir(empresa, "conta-a", new BigDecimal("1500.00"), Direcao.CREDITO);
        Transacao debito = persistir(empresa, "conta-b", new BigDecimal("1500.00"), Direcao.DEBITO);

        int pares = transferencias.detectar(empresa);

        assertThat(pares).isEqualTo(1);
        Transacao creditoAtual = buscar(empresa, credito.getId());
        Transacao debitoAtual = buscar(empresa, debito.getId());
        assertThat(creditoAtual.getClasse()).isEqualTo(ClasseTransacao.TRANSFERENCIA_INTERNA);
        assertThat(debitoAtual.getClasse()).isEqualTo(ClasseTransacao.TRANSFERENCIA_INTERNA);
        assertThat(creditoAtual.getTransferParId()).isEqualTo(debito.getId());
        assertThat(debitoAtual.getTransferParId()).isEqualTo(credito.getId());
        assertThat(creditoAtual.getTransferOrigem()).isEqualTo(OrigemTransferencia.AUTOMATICA);

        assertThat(transacoes.listarNaoPareadas(empresa, Pageable.unpaged()).getTotalElements())
                .isZero();
        assertThat(transacoes.listarPorEmpresa(empresa, Pageable.unpaged()).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    void naoPareiaQuandoHaAmbiguidade() {
        UUID empresa = cadastrarEmpresa("ambiguo@example.com");
        persistir(empresa, "conta-a", new BigDecimal("100.00"), Direcao.CREDITO);
        persistir(empresa, "conta-b", new BigDecimal("100.00"), Direcao.CREDITO);
        persistir(empresa, "conta-c", new BigDecimal("100.00"), Direcao.DEBITO);

        int pares = transferencias.detectar(empresa);

        assertThat(pares).isZero();
        assertThat(transacoes.listarNaoPareadas(empresa, Pageable.unpaged()).getTotalElements())
                .isEqualTo(3);
    }

    @Test
    void naoPareiaMovimentosDaMesmaConta() {
        UUID empresa = cadastrarEmpresa("mesma-conta@example.com");
        persistir(empresa, "conta-a", new BigDecimal("70.00"), Direcao.CREDITO);
        persistir(empresa, "conta-a", new BigDecimal("70.00"), Direcao.DEBITO);

        int pares = transferencias.detectar(empresa);

        assertThat(pares).isZero();
    }

    @Test
    void deteccaoEhIdempotente() {
        UUID empresa = cadastrarEmpresa("idempotente@example.com");
        persistir(empresa, "conta-a", new BigDecimal("900.00"), Direcao.CREDITO);
        persistir(empresa, "conta-b", new BigDecimal("900.00"), Direcao.DEBITO);

        assertThat(transferencias.detectar(empresa)).isEqualTo(1);
        assertThat(transferencias.detectar(empresa)).isZero();
    }

    @Test
    void desfazerVoltaAsDuasPernasParaRevisao() {
        UUID empresa = cadastrarEmpresa("desfazer@example.com");
        Transacao credito = persistir(empresa, "conta-a", new BigDecimal("320.00"), Direcao.CREDITO);
        Transacao debito = persistir(empresa, "conta-b", new BigDecimal("320.00"), Direcao.DEBITO);
        transferencias.detectar(empresa);

        transferencias.desfazer(empresa, credito.getId());

        Transacao creditoAtual = buscar(empresa, credito.getId());
        Transacao debitoAtual = buscar(empresa, debito.getId());
        assertThat(creditoAtual.getEstado()).isEqualTo(EstadoTransacao.EM_REVISAO);
        assertThat(debitoAtual.getEstado()).isEqualTo(EstadoTransacao.EM_REVISAO);
        assertThat(creditoAtual.getTransferParId()).isNull();
        assertThat(debitoAtual.getTransferParId()).isNull();
        assertThat(transacoes.listarNaoPareadas(empresa, Pageable.unpaged()).getTotalElements())
                .isEqualTo(2);
    }

    private Transacao persistir(UUID empresa, String conta, BigDecimal valor, Direcao direcao) {
        String idExterno = "tx-" + sequencia.incrementAndGet();
        Transacao transacao = Transacao.ingerida(new DadosTransacao(
                empresa,
                FonteIntegracao.PLUGGY,
                idExterno,
                conta,
                conta,
                DIA,
                valor,
                direcao,
                "movimento",
                null,
                null));
        ClasseTransacao classe = direcao == Direcao.CREDITO
                ? ClasseTransacao.CREDITO_VENDA
                : ClasseTransacao.FLORES_FOLHAGENS_PLANTAS;
        transacao.classificar(classe, Confianca.de(new BigDecimal("0.950")), "regra");
        return tx.execute(status -> transacoes.salvar(transacao));
    }

    private Transacao buscar(UUID empresa, UUID id) {
        return transacoes.buscarPorId(empresa, id).orElseThrow();
    }

    private UUID cadastrarEmpresa(String email) {
        return cadastrar.executar(new CadastrarEmpresaCommand(
                "Empresa", null, "Ana", email, "senha-supersegura")).empresaId();
    }

    private void limparBanco() {
        jdbc.update("DELETE FROM transacao");
        jdbc.update("DELETE FROM integracao_pluggy");
        jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM empresa");
    }
}
