package com.planteumaflor.conciliador.revisao.query;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class RevisaoQueryService implements ConsultarFilaRevisao {

    private static final RowMapper<FilaRevisaoItemView> ITEM_MAPPER =
            new FilaRevisaoItemMapper();

    private final TransacaoRepository transacoes;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    RevisaoQueryService(TransacaoRepository transacoes, JdbcTemplate jdbc, EntityManager entityManager) {
        this.transacoes = transacoes;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FilaRevisaoItemView> consultar(
            UUID empresaId, FiltroRevisao filtro, Pageable pageable) {
        entityManager.flush();
        QueryParts query = montarWhere(empresaId, filtro);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transacao" + query.where(),
                Long.class,
                query.params().toArray());
        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Object> params = new ArrayList<>(query.params());
        StringBuilder sql = new StringBuilder("""
                SELECT id, version, data, conta_local, descricao_raw, direcao,
                       valor_liquido, classe, confianca, motivo_revisao, estado
                FROM transacao
                """)
                .append(query.where())
                .append(" ORDER BY data ASC, created_at ASC");
        if (pageable.isPaged()) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(pageable.getPageSize());
            params.add(pageable.getOffset());
        }

        List<FilaRevisaoItemView> itens = jdbc.query(
                sql.toString(),
                ITEM_MAPPER,
                params.toArray());
        return new PageImpl<>(itens, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilaRevisaoItemView> consultarItem(UUID empresaId, UUID transacaoId) {
        return transacoes.buscarPorId(empresaId, transacaoId).map(this::paraView);
    }

    private QueryParts montarWhere(UUID empresaId, FiltroRevisao filtro) {
        StringBuilder where = new StringBuilder(" WHERE empresa_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);

        if (filtro.estado() != null) {
            where.append(" AND estado = ?");
            params.add(filtro.estado().name());
        }
        if (filtro.direcao() != null) {
            where.append(" AND direcao = ?");
            params.add(filtro.direcao().name());
        }
        if (filtro.classe() != null) {
            where.append(" AND classe = ?");
            params.add(filtro.classe().name());
        } else {
            // Oculta as pernas já pareadas como transferência interna, a menos que o
            // usuário filtre explicitamente por essa classe.
            where.append(" AND transfer_par_id IS NULL");
        }

        List<String> termos = filtro.termosBusca();
        if (!termos.isEmpty()) {
            where.append(" AND (");
            List<String> condicoes = new ArrayList<>();
            for (String termo : termos) {
                String like = "%" + termo + "%";
                condicoes.add("""
                        LOWER(COALESCE(descricao_raw, '')) LIKE ?
                        OR LOWER(COALESCE(conta_local, '')) LIKE ?
                        OR LOWER(COALESCE(id_transacao_externa, '')) LIKE ?
                        OR LOWER(COALESCE(id_conta_externa, '')) LIKE ?
                        OR LOWER(COALESCE(contraparte_doc, '')) LIKE ?
                        OR LOWER(COALESCE(e2e_id, '')) LIKE ?
                        """);
                for (int i = 0; i < 6; i++) {
                    params.add(like);
                }
            }
            where.append(String.join(" OR ", condicoes));
            where.append(")");
        }
        return new QueryParts(where.toString(), params);
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

    private record QueryParts(String where, List<Object> params) {}

    private static final class FilaRevisaoItemMapper implements RowMapper<FilaRevisaoItemView> {
        @Override
        public FilaRevisaoItemView mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new FilaRevisaoItemView(
                    rs.getObject("id", UUID.class),
                    rs.getLong("version"),
                    rs.getObject("data", LocalDate.class),
                    rs.getString("conta_local"),
                    rs.getString("descricao_raw"),
                    Direcao.valueOf(rs.getString("direcao")),
                    rs.getObject("valor_liquido", BigDecimal.class),
                    ClasseTransacao.valueOf(rs.getString("classe")),
                    rs.getObject("confianca", BigDecimal.class),
                    rs.getString("motivo_revisao"),
                    EstadoTransacao.valueOf(rs.getString("estado")));
        }
    }
}
