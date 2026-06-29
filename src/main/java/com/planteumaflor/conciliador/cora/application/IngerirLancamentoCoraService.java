package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.classificacao.Classificador;
import com.planteumaflor.conciliador.conta.application.ContaBancariaService;
import com.planteumaflor.conciliador.conta.domain.ContaBancaria;
import com.planteumaflor.conciliador.conta.domain.TipoContaBancaria;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Normaliza, classifica e insere um lançamento em uma transação curta. */
@Service
class IngerirLancamentoCoraService {

    private final TransacaoRepository transacoes;
    private final Classificador classificador;
    private final ContaBancariaService contas;

    IngerirLancamentoCoraService(
            TransacaoRepository transacoes,
            Classificador classificador,
            ContaBancariaService contas) {
        this.transacoes = transacoes;
        this.classificador = classificador;
        this.contas = contas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean ingerir(UUID empresaId, CoraGateway.LancamentoExtrato lancamento) {
        Direcao direcao = lancamento.valor().signum() < 0 ? Direcao.DEBITO : Direcao.CREDITO;
        String contaIdExterna = textoOuPadrao(lancamento.idContaExterna(), "cora");
        ContaBancaria conta = contas.salvarOuAtualizar(
                empresaId,
                FonteIntegracao.CORA,
                contaIdExterna,
                "Cora",
                null,
                null,
                null,
                null,
                TipoContaBancaria.CORRENTE);
        if (!conta.isAtiva()) {
            return false;
        }
        Transacao transacao = Transacao.ingerida(new DadosTransacao(
                empresaId,
                FonteIntegracao.CORA,
                lancamento.idTransacaoExterna(),
                contaIdExterna,
                "cora",
                lancamento.data(),
                lancamento.valor().abs(),
                direcao,
                lancamento.descricao(),
                lancamento.contraparteDoc(),
                lancamento.e2eId()));
        classificador.classificar(transacao);
        return transacoes.inserirSeAusente(transacao);
    }

    private String textoOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.strip();
    }
}
