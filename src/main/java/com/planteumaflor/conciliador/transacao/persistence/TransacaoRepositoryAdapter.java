package com.planteumaflor.conciliador.transacao.persistence;

import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class TransacaoRepositoryAdapter implements TransacaoRepository {

    private final SpringDataTransacaoRepository springData;

    TransacaoRepositoryAdapter(SpringDataTransacaoRepository springData) {
        this.springData = springData;
    }

    @Override
    public Transacao salvar(Transacao transacao) {
        return springData.save(transacao);
    }

    @Override
    public Optional<Transacao> buscarPorId(UUID empresaId, UUID transacaoId) {
        return springData.findByIdAndEmpresaId(transacaoId, empresaId);
    }

    @Override
    public boolean existePorOrigem(UUID empresaId, String pluggyTransactionId) {
        return springData.existsByEmpresaIdAndPluggyTransactionId(
                empresaId, pluggyTransactionId);
    }
}
