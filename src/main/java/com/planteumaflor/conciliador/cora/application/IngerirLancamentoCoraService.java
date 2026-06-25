package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.classificacao.Classificador;
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

    IngerirLancamentoCoraService(TransacaoRepository transacoes, Classificador classificador) {
        this.transacoes = transacoes;
        this.classificador = classificador;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean ingerir(UUID empresaId, CoraGateway.LancamentoExtrato lancamento) {
        Direcao direcao = lancamento.valor().signum() < 0 ? Direcao.DEBITO : Direcao.CREDITO;
        Transacao transacao = Transacao.ingerida(new DadosTransacao(
                empresaId,
                FonteIntegracao.CORA,
                lancamento.idTransacaoExterna(),
                lancamento.idContaExterna(),
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
}
