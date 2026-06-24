package com.planteumaflor.conciliador.transacao.persistence;

import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public boolean existePorOrigem(UUID empresaId, FonteIntegracao fonte, String idTransacaoExterna) {
        return springData.existsByEmpresaIdAndFonteAndIdTransacaoExterna(
                empresaId, fonte, idTransacaoExterna);
    }

    @Override
    public Page<Transacao> listarPorEmpresa(UUID empresaId, Pageable pageable) {
        return springData.findByEmpresaId(empresaId, pageable);
    }
}
