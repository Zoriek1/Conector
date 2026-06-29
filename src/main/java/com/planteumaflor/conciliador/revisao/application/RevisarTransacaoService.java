package com.planteumaflor.conciliador.revisao.application;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Transactional
class RevisarTransacaoService implements RevisarTransacao {

    private final TransacaoRepository transacoes;

    RevisarTransacaoService(TransacaoRepository transacoes) {
        this.transacoes = transacoes;
    }

    @Override
    public void aprovar(UUID empresaId, UUID transacaoId, long versaoEsperada) {
        aplicar(empresaId, transacaoId, versaoEsperada, Transacao::aprovarParaApi);
    }

    @Override
    public void classificar(
            UUID empresaId, UUID transacaoId, long versaoEsperada, ClasseTransacao classe) {
        aplicar(empresaId, transacaoId, versaoEsperada,
                t -> t.reclassificarManualmente(classe, "classificação manual na revisão"));
    }

    @Override
    public void selecionarMatch(
            UUID empresaId,
            UUID transacaoId,
            long versaoEsperada,
            String tipo,
            String idExterno,
            BigDecimal taxaDerivada) {
        aplicar(empresaId, transacaoId, versaoEsperada,
                t -> t.registrarMatch(tipo, idExterno, taxaDerivada));
    }

    @Override
    public void rotearParaOfx(UUID empresaId, UUID transacaoId, long versaoEsperada) {
        aplicar(empresaId, transacaoId, versaoEsperada, Transacao::rotearParaOfx);
    }

    @Override
    public void solicitarRetry(UUID empresaId, UUID transacaoId, long versaoEsperada) {
        aplicar(empresaId, transacaoId, versaoEsperada, Transacao::tentarNovamenteApi);
    }

    private void aplicar(
            UUID empresaId, UUID transacaoId, long versaoEsperada, Consumer<Transacao> comando) {
        Transacao transacao = transacoes.buscarPorId(empresaId, transacaoId)
                .orElseThrow(() -> new RecursoNaoEncontrado(
                        "transação não encontrada para a empresa"));
        if (transacao.getVersion() != versaoEsperada) {
            throw new ConflitoDeVersao(
                    "a transação foi alterada por outra ação; recarregue a fila");
        }
        comando.accept(transacao);
        transacoes.salvar(transacao);
    }
}
