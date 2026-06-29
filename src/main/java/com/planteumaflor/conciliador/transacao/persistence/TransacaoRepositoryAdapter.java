package com.planteumaflor.conciliador.transacao.persistence;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransacaoRepositoryAdapter implements TransacaoRepository {

    private final SpringDataTransacaoRepository springData;
    private final JdbcTemplate jdbc;

    TransacaoRepositoryAdapter(SpringDataTransacaoRepository springData, JdbcTemplate jdbc) {
        this.springData = springData;
        this.jdbc = jdbc;
    }

    @Override
    public Transacao salvar(Transacao transacao) {
        return springData.save(transacao);
    }

    @Override
    public boolean inserirSeAusente(Transacao transacao) {
        int inseridas = jdbc.update("""
                INSERT INTO transacao (
                    empresa_id, fonte, id_transacao_externa, id_conta_externa,
                    conta_local, data, valor_liquido, direcao, descricao_raw,
                    contraparte_doc, e2e_id, classe, confianca,
                    justificativa_classificacao, motivo_revisao, estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (empresa_id, fonte, id_transacao_externa) DO NOTHING
                """,
                transacao.getEmpresaId(),
                transacao.getFonte().name(),
                transacao.getIdTransacaoExterna(),
                transacao.getIdContaExterna(),
                transacao.getContaLocal(),
                transacao.getData(),
                transacao.getValorLiquido(),
                transacao.getDirecao().name(),
                transacao.getDescricaoRaw(),
                transacao.getContraparteDoc(),
                transacao.getE2eId(),
                transacao.getClasse().name(),
                transacao.getConfianca().valor(),
                transacao.getJustificativaClassificacao(),
                transacao.getMotivoRevisao(),
                transacao.getEstado().name());
        return inseridas == 1;
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

    @Override
    public Page<Transacao> listarNaoPareadas(UUID empresaId, Pageable pageable) {
        return springData.findByEmpresaIdAndTransferParIdIsNull(empresaId, pageable);
    }

    @Override
    public Page<Transacao> listarPorEstado(
            UUID empresaId, EstadoTransacao estado, Pageable pageable) {
        return springData.findByEmpresaIdAndEstado(empresaId, estado, pageable);
    }

    @Override
    public List<Transacao> listarCandidatosTransferencia(UUID empresaId) {
        return springData.findByEmpresaIdAndTransferParIdIsNullAndEstadoInAndClasseNot(
                empresaId,
                List.of(EstadoTransacao.CLASSIFICADO, EstadoTransacao.EM_REVISAO),
                ClasseTransacao.TRANSFERENCIA_INTERNA);
    }
}
