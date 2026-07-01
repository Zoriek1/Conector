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
    void classificaDebitoComDescricaoDeTarifaComoFinanceira() {
        Transacao transacao = transacao(Direcao.DEBITO, "Tarifa de manutenção de conta");

        classificador.classificar(transacao);

        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.CLASSIFICADO);
        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.TARIFAS_BANCARIAS);
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

        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.AGUA_ENERGIA_INTERNET_TELEFONE);
        assertThat(transacao.getEstado()).isEqualTo(EstadoTransacao.CLASSIFICADO);
    }

    @Test
    void classificaFornecedorComoCmv() {
        Transacao transacao = transacao(Direcao.DEBITO, "Pagamento fornecedor de flores");

        classificador.classificar(transacao);

        assertThat(transacao.getClasse()).isEqualTo(ClasseTransacao.FLORES_FOLHAGENS_PLANTAS);
    }

    @Test
    void classificaImpostoMarketingFreteESistemas() {
        Transacao imposto = transacao(Direcao.DEBITO, "DAS Simples Nacional");
        Transacao marketing = transacao(Direcao.DEBITO, "Facebook Ads campanha");
        Transacao frete = transacao(Direcao.DEBITO, "Correios frete pedido");
        Transacao sistema = transacao(Direcao.DEBITO, "Assinatura Nuvemshop");

        classificador.classificar(imposto);
        classificador.classificar(marketing);
        classificador.classificar(frete);
        classificador.classificar(sistema);

        assertThat(imposto.getClasse()).isEqualTo(ClasseTransacao.IMPOSTOS_TRIBUTOS);
        assertThat(marketing.getClasse()).isEqualTo(ClasseTransacao.MARKETING_TRAFEGO);
        assertThat(frete.getClasse()).isEqualTo(ClasseTransacao.FRETE_ENTREGAS);
        assertThat(sistema.getClasse()).isEqualTo(ClasseTransacao.SISTEMAS_ASSINATURAS);
    }

    @Test
    void classificaEstornoVendaAlimentacaoEVeiculo() {
        Transacao estorno = transacao(Direcao.DEBITO, "Chargeback venda cancelada cliente");
        Transacao alimentacao = transacao(Direcao.DEBITO, "Almoco equipe restaurante");
        Transacao combustivel = transacao(Direcao.DEBITO, "Posto gasolina abastecimento");
        Transacao manutencao = transacao(Direcao.DEBITO, "Oficina troca de oleo veiculo");
        Transacao estacionamento = transacao(Direcao.DEBITO, "Estacionamento entrega cliente");

        classificador.classificar(estorno);
        classificador.classificar(alimentacao);
        classificador.classificar(combustivel);
        classificador.classificar(manutencao);
        classificador.classificar(estacionamento);

        assertThat(estorno.getClasse()).isEqualTo(ClasseTransacao.VENDA_CANCELADA_ESTORNO);
        assertThat(alimentacao.getClasse()).isEqualTo(ClasseTransacao.ALIMENTACAO_EQUIPE);
        assertThat(combustivel.getClasse()).isEqualTo(ClasseTransacao.COMBUSTIVEL_VEICULO);
        assertThat(manutencao.getClasse()).isEqualTo(ClasseTransacao.MANUTENCAO_VEICULO);
        assertThat(estacionamento.getClasse()).isEqualTo(ClasseTransacao.PEDAGIO_ESTACIONAMENTO);
    }

    @Test
    void classificaCategoriasForaDoDre() {
        Transacao aporte = transacao(Direcao.CREDITO, "Aporte de capital social");
        Transacao retirada = transacao(Direcao.DEBITO, "Retirada socio");

        classificador.classificar(aporte);
        classificador.classificar(retirada);

        assertThat(aporte.getClasse()).isEqualTo(ClasseTransacao.APORTE_SOCIO);
        assertThat(retirada.getClasse()).isEqualTo(ClasseTransacao.RETIRADA_DISTRIBUICAO_SOCIOS);
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
                new ConciliadorProperties.Bling(null, null, null, null, null, null, Duration.ofMinutes(2)),
                new ConciliadorProperties.Pluggy(null, null, null),
                new ConciliadorProperties.Cora(null, null, null, null, null),
                new ConciliadorProperties.Cripto(null));
    }
}
