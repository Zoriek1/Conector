package com.planteumaflor.conciliador.revisao.application;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                t -> t.reclassificarManualmente(classe, "classificacao manual na revisao"));
    }

    @Override
    public void classificarLote(UUID empresaId, ClassificarLoteCommand comando) {
        if (comando.itens().isEmpty()) {
            throw new IllegalArgumentException("selecione ao menos uma transacao");
        }
        if (comando.classe() == ClasseTransacao.INDEFINIDO) {
            throw new IllegalArgumentException("classe indefinida nao pode ser aplicada em lote");
        }

        List<Transacao> itens = carregarEValidarLote(empresaId, comando);
        String justificativa = comando.justificativa() == null || comando.justificativa().isBlank()
                ? "classificacao manual em lote"
                : comando.justificativa().strip();

        for (Transacao transacao : itens) {
            transacao.reclassificarManualmente(comando.classe(), justificativa);
            transacoes.salvar(transacao);
        }
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
                        "transacao nao encontrada para a empresa"));
        if (transacao.getVersion() != versaoEsperada) {
            throw new ConflitoDeVersao(
                    "a transacao foi alterada por outra acao; recarregue a fila");
        }
        comando.accept(transacao);
        transacoes.salvar(transacao);
    }

    private List<Transacao> carregarEValidarLote(UUID empresaId, ClassificarLoteCommand comando) {
        List<Transacao> transacoesValidas = new ArrayList<>();
        Set<UUID> ids = new HashSet<>();
        for (ClassificarLoteCommand.ItemVersao item : comando.itens()) {
            if (!ids.add(item.transacaoId())) {
                throw new IllegalArgumentException("a selecao contem transacao duplicada");
            }
            Transacao transacao = transacoes.buscarPorId(empresaId, item.transacaoId())
                    .orElseThrow(() -> new RecursoNaoEncontrado(
                            "transacao nao encontrada para a empresa"));
            if (transacao.getVersion() != item.version()) {
                throw new ConflitoDeVersao(
                        "uma transacao selecionada foi alterada; recarregue a fila");
            }
            if (transacao.getEstado() != EstadoTransacao.EM_REVISAO
                    && transacao.getEstado() != EstadoTransacao.CLASSIFICADO) {
                throw new IllegalStateException(
                        "lote so pode classificar itens em revisao ou classificados");
            }
            if (!comando.classe().aceita(transacao.getDirecao())) {
                throw new IllegalArgumentException(
                        "classe " + comando.classe().name()
                                + " nao e compativel com " + transacao.getDirecao().name());
            }
            transacoesValidas.add(transacao);
        }
        return transacoesValidas;
    }
}
