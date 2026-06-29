package com.planteumaflor.conciliador.revisao.query;

import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
class RevisaoQueryService implements ConsultarFilaRevisao {

    private final TransacaoRepository transacoes;

    RevisaoQueryService(TransacaoRepository transacoes) {
        this.transacoes = transacoes;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FilaRevisaoItemView> consultar(
            UUID empresaId, EstadoTransacao estado, Pageable pageable) {
        return transacoes.listarPorEstado(empresaId, estado, pageable)
                .map(this::paraView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilaRevisaoItemView> consultarItem(UUID empresaId, UUID transacaoId) {
        return transacoes.buscarPorId(empresaId, transacaoId).map(this::paraView);
    }

    private FilaRevisaoItemView paraView(Transacao transacao) {
        return new FilaRevisaoItemView(
                transacao.getId(),
                transacao.getVersion(),
                transacao.getData(),
                transacao.getContaLocal(),
                transacao.getDescricaoRaw(),
                transacao.getDirecao(),
                transacao.getValorLiquido(),
                transacao.getClasse(),
                transacao.getConfianca().valor(),
                transacao.getMotivoRevisao(),
                transacao.getEstado());
    }
}
