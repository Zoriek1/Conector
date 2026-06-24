package com.planteumaflor.conciliador.transacao.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransacaoTest {

    @Test
    void criaMovimentoIngeridoComDinheiroNormalizado() {
        Transacao transacao = Transacao.ingerida(dados(new BigDecimal("10.015")));

        assertThat(transacao.getValorLiquido()).isEqualByComparingTo("10.02");
        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.INDEFINIDO);
        assertThat(transacao.getConfianca()).isEqualTo(Confianca.zero());
        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.INGERIDO);
    }

    @Test
    void rejeitaValorNuloZeroOuNegativo() {
        assertThatThrownBy(() -> Transacao.ingerida(dados(null)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Transacao.ingerida(dados(BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Transacao.ingerida(dados(new BigDecimal("-0.01"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confiancaValidaFaixaENormalizaEscala() {
        assertThat(Confianca.de(new BigDecimal("0.9004")).valor())
                .isEqualByComparingTo("0.900");
        assertThatThrownBy(() -> Confianca.de(new BigDecimal("-0.001")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Confianca.de(new BigDecimal("1.001")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void percorreFluxoApiAteConciliado() {
        Transacao transacao = Transacao.ingerida(dados(new BigDecimal("100.00")));

        transacao.classificar(
                ClasseTransacao.CREDITO_VENDA,
                Confianca.de(new BigDecimal("0.950")),
                "regra de recebimento");
        transacao.registrarMatch("conta_receber", "bling-123", new BigDecimal("5.005"));
        transacao.aprovarParaApi();
        transacao.registrarEscrita("bordero-123");
        transacao.conciliar();

        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.CONCILIADO);
        assertThat(transacao.getBlingBorderoId()).isEqualTo("bordero-123");
        assertThatThrownBy(transacao::conciliar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void percorreFluxosDeRevisaoOfxEFalhaComRetry() {
        Transacao ofx = Transacao.ingerida(dados(new BigDecimal("50.00")));
        ofx.enviarParaRevisao("dados insuficientes");
        ofx.rotearParaOfx();
        ofx.associarLoteOfx("lote-1");
        ofx.conciliar();
        assertThat(ofx.getEstado()).isEqualTo(EstadoTransacao.CONCILIADO);

        Transacao api = Transacao.ingerida(dados(new BigDecimal("60.00")));
        api.classificar(ClasseTransacao.DEBITO_DESPESA, Confianca.de(new BigDecimal("0.990")), "regra");
        api.aprovarParaApi();
        api.registrarFalha();
        api.tentarNovamenteApi();
        assertThat(api.getEstado()).isEqualTo(EstadoTransacao.AGUARDANDO_ESCRITA_API);
    }

    @Test
    void impedeTransicoesInvalidas() {
        Transacao transacao = Transacao.ingerida(dados(new BigDecimal("20.00")));

        assertThatThrownBy(transacao::aprovarParaApi)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> transacao.registrarEscrita("bordero"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(transacao::conciliar)
                .isInstanceOf(IllegalStateException.class);
    }

    private DadosTransacao dados(BigDecimal valor) {
        return new DadosTransacao(
                UUID.randomUUID(),
                "pluggy-transaction-1",
                "pluggy-account-1",
                "cora",
                LocalDate.of(2026, 6, 23),
                valor,
                Direcao.CREDITO,
                "Recebimento",
                "12345678901",
                "E2E-1");
    }
}
