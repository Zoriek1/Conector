package com.planteumaflor.conciliador.classificacao;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasClassificadorTest {

    private final RegrasClassificador classificador = new RegrasClassificador(propriedades());

    @Test
    void classificaCreditoComDescricaoDeRecebimentoComoVenda() {
        Transacao transacao = transacao(Direcao.CREDITO, "PIX recebido de cliente");

        classificador.classificar(transacao);

        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.CLASSIFICADO);
        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.CREDITO_VENDA);
    }

    @Test
    void classificaDebitoComDescricaoDeTaxaComoDespesa() {
        Transacao transacao = transacao(Direcao.DEBITO, "Tarifa de manutenção de conta");

        classificador.classificar(transacao);

        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.CLASSIFICADO);
        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.DEBITO_DESPESA);
    }

    @Test
    void classificaTransferenciaEntreContasProprias() {
        Transacao transacao = transacao(Direcao.DEBITO, "Transferência entre contas mesmo CNPJ");

        classificador.classificar(transacao);

        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.TRANSFERENCIA_INTERNA);
    }

    @Test
    void classificaProLabore() {
        Transacao transacao = transacao(Direcao.DEBITO, "Pagamento de pró-labore sócio");

        classificador.classificar(transacao);

        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.PRO_LABORE);
    }

    @Test
    void enviaParaRevisaoQuandoNenhumaRegraCorresponde() {
        Transacao transacao = transacao(Direcao.CREDITO, "Lançamento sem descrição clara");

        classificador.classificar(transacao);

        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.EM_REVISAO);
    }

    @Test
    void normalizaAcentosEPontuacaoAntesDeAplicarRegra() {
        Transacao transacao = transacao(Direcao.DEBITO, "PAGAMENTO: ENERGIA ELÉTRICA");

        classificador.classificar(transacao);

        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.DEBITO_DESPESA);
        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.CLASSIFICADO);
    }

    @Test
    void naoClassificaProLaboreComoCredito() {
        Transacao transacao = transacao(Direcao.CREDITO, "Pró-labore recebido");

        classificador.classificar(transacao);

        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.EM_REVISAO);
    }

    private Transacao transacao(Direcao direcao, String descricao) {
        return Transacao.ingerida(new DadosTransacao(
                UUID.randomUUID(),
                FonteIntegracao.CORA,
                "ext-" + UUID.randomUUID(),
                "conta-1",
                "cora",
                LocalDate.of(2026, 6, 23),
                new BigDecimal("100.00"),
                direcao,
                descricao,
                null,
                null));
    }

    private ConciliadorProperties propriedades() {
        return new ConciliadorProperties(
                "America/Sao_Paulo",
                new ConciliadorProperties.Ingest("0 0 4 * * *", 7, 3, Duration.ofSeconds(2)),
                new ConciliadorProperties.Classificacao(
                        new BigDecimal("0.900"), new BigDecimal("0.100")),
                new ConciliadorProperties.Bling(null, null, null, Duration.ofMinutes(2)),
                new ConciliadorProperties.Pluggy(null),
                new ConciliadorProperties.Cora(null, null, null, null, null),
                new ConciliadorProperties.Cripto(null));
    }
}
