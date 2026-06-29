package com.planteumaflor.conciliador.revisao.application;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevisarTransacaoServiceTest {

    private static final UUID EMPRESA = UUID.randomUUID();

    private final RepositorioFake repositorio = new RepositorioFake();
    private final RevisarTransacaoService servico = new RevisarTransacaoService(repositorio);

    @Test
    void aprovarAplicaTransicaoEPersiste() {
        Transacao item = emRevisao();
        repositorio.guardar(item);

        servico.aprovar(EMPRESA, item.getId(), item.getVersion());

        assertThat(item.getEstado()).isEqualTo(EstadoTransacao.AGUARDANDO_ESCRITA_API);
        assertThat(repositorio.salvou).isTrue();
    }

    @Test
    void classificarReclassificaManualmente() {
        Transacao item = emRevisao();
        repositorio.guardar(item);

        servico.classificar(EMPRESA, item.getId(), item.getVersion(), ClasseTransacao.DEBITO_DESPESA);

        assertThat(item.getEstado()).isEqualTo(EstadoTransacao.CLASSIFICADO);
        assertThat(item.getClasse()).isEqualTo(ClasseTransacao.DEBITO_DESPESA);
    }

    @Test
    void versaoDivergenteLancaConflito() {
        Transacao item = emRevisao();
        repositorio.guardar(item);

        assertThatThrownBy(() -> servico.aprovar(EMPRESA, item.getId(), item.getVersion() + 1))
                .isInstanceOf(ConflitoDeVersao.class);
        assertThat(repositorio.salvou).isFalse();
    }

    @Test
    void itemInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> servico.aprovar(EMPRESA, UUID.randomUUID(), 0))
                .isInstanceOf(RecursoNaoEncontrado.class);
    }

    @Test
    void transicaoInvalidaPropagaExcecaoDeDominio() {
        Transacao item = emRevisao();
        repositorio.guardar(item);

        // retry exige FALHA; em EM_REVISAO a transição é inválida.
        assertThatThrownBy(() -> servico.solicitarRetry(EMPRESA, item.getId(), item.getVersion()))
                .isInstanceOf(IllegalStateException.class);
    }

    private Transacao emRevisao() {
        Transacao transacao = Transacao.ingerida(new DadosTransacao(
                EMPRESA,
                FonteIntegracao.CORA,
                "ext-" + UUID.randomUUID(),
                "conta-cora",
                "cora",
                LocalDate.of(2026, 6, 24),
                new BigDecimal("25.00"),
                Direcao.CREDITO,
                "descricao",
                null,
                null));
        transacao.enviarParaRevisao("nenhuma regra correspondeu");
        return transacao;
    }

    /** Repositório em memória mínimo; só os métodos usados pelo serviço. */
    private static final class RepositorioFake implements TransacaoRepository {

        private Transacao armazenado;
        private boolean salvou;

        void guardar(Transacao transacao) {
            this.armazenado = transacao;
        }

        @Override
        public Transacao salvar(Transacao transacao) {
            this.salvou = true;
            return transacao;
        }

        @Override
        public Optional<Transacao> buscarPorId(UUID empresaId, UUID transacaoId) {
            // O id é gerado pelo JPA; no teste unitário casamos pela empresa.
            return armazenado != null && armazenado.getEmpresaId().equals(empresaId)
                    ? Optional.of(armazenado)
                    : Optional.empty();
        }

        @Override
        public boolean inserirSeAusente(Transacao transacao) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existePorOrigem(
                UUID empresaId, FonteIntegracao fonte, String idTransacaoExterna) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Transacao> listarPorEmpresa(UUID empresaId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Transacao> listarPorEstado(
                UUID empresaId, EstadoTransacao estado, Pageable pageable) {
            throw new UnsupportedOperationException();
        }
    }
}
