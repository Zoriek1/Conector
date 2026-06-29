package com.planteumaflor.conciliador.pluggy.application;

import com.planteumaflor.conciliador.classificacao.Classificador;
import com.planteumaflor.conciliador.transacao.domain.DadosTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
class IngerirTransacaoPluggyService {

    private final TransacaoRepository transacoes;
    private final Classificador classificador;

    IngerirTransacaoPluggyService(TransacaoRepository transacoes, Classificador classificador) {
        this.transacoes = transacoes;
        this.classificador = classificador;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean ingerir(
            UUID empresaId,
            PluggyGateway.ContaPluggy conta,
            PluggyGateway.TransacaoPluggy transacaoPluggy) {
        Direcao direcao = direcao(transacaoPluggy);
        BigDecimal valor = transacaoPluggy.valor().abs();
        Transacao transacao = Transacao.ingerida(new DadosTransacao(
                empresaId,
                FonteIntegracao.PLUGGY,
                transacaoPluggy.id(),
                conta.id(),
                conta.nome(),
                transacaoPluggy.data(),
                valor,
                direcao,
                transacaoPluggy.descricao(),
                transacaoPluggy.documentoContraparte(),
                transacaoPluggy.providerCode()));
        classificador.classificar(transacao);
        return transacoes.inserirSeAusente(transacao);
    }

    private Direcao direcao(PluggyGateway.TransacaoPluggy transacao) {
        String tipo = transacao.tipo() == null ? "" : transacao.tipo().toUpperCase(Locale.ROOT);
        if (tipo.equals("CREDIT")) {
            return Direcao.CREDITO;
        }
        if (tipo.equals("DEBIT")) {
            return Direcao.DEBITO;
        }
        return transacao.valor().signum() < 0 ? Direcao.DEBITO : Direcao.CREDITO;
    }
}
