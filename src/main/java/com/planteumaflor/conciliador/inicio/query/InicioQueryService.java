package com.planteumaflor.conciliador.inicio.query;

import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.integracoes.RotulosIntegracao;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class InicioQueryService implements ConsultarInicio {

    private final JdbcTemplate jdbc;
    private final IntegracaoCoraJpaRepository cora;
    private final IntegracaoPluggyJpaRepository pluggy;
    private final Clock clock;

    InicioQueryService(
            JdbcTemplate jdbc,
            IntegracaoCoraJpaRepository cora,
            IntegracaoPluggyJpaRepository pluggy,
            Clock clock) {
        this.jdbc = jdbc;
        this.cora = cora;
        this.pluggy = pluggy;
        this.clock = clock;
    }

    @Override
    public InicioView consultar(UUID empresaId) {
        Map<String, Long> contagens = contagens(empresaId);
        return new InicioView(
                contagens.getOrDefault("EM_REVISAO", 0L),
                contagens.getOrDefault("FALHA", 0L),
                contagens.getOrDefault("AGUARDANDO_ESCRITA_API", 0L),
                contagens.getOrDefault("EM_LOTE_OFX", 0L),
                contagens.getOrDefault("CONCILIADO", 0L),
                integracoes(empresaId),
                atividades(empresaId),
                clock.instant());
    }

    private Map<String, Long> contagens(UUID empresaId) {
        Map<String, Long> resultado = new HashMap<>();
        jdbc.query("""
                SELECT estado, count(*) total
                  FROM transacao
                 WHERE empresa_id = ?
                 GROUP BY estado
                """, rs -> {
            resultado.put(rs.getString("estado"), rs.getLong("total"));
        }, empresaId);
        return resultado;
    }

    private List<InicioView.IntegracaoStatusView> integracoes(UUID empresaId) {
        List<InicioView.IntegracaoStatusView> itens = new ArrayList<>();
        cora.findByEmpresaId(empresaId)
                .ifPresentOrElse(i -> itens.add(new InicioView.IntegracaoStatusView(
                                "Cora",
                                i.getStatus().name(),
                                RotulosIntegracao.status(i.getStatus().name()),
                                i.getUltimaSincronizacao(),
                                RotulosIntegracao.falha(i.getUltimaFalhaTipo() == null ? null : i.getUltimaFalhaTipo().name()),
                                i.getFalhasConsecutivas())),
                        () -> itens.add(new InicioView.IntegracaoStatusView(
                                "Cora", "NAO_CONECTADA", RotulosIntegracao.status("NAO_CONECTADA"), null, null, 0)));
        pluggy.findByEmpresaId(empresaId)
                .ifPresentOrElse(i -> itens.add(new InicioView.IntegracaoStatusView(
                                "Pluggy",
                                i.getStatus().name(),
                                RotulosIntegracao.status(i.getStatus().name()),
                                i.getUltimaSincronizacao(),
                                RotulosIntegracao.falha(i.getUltimaFalhaTipo()),
                                i.getFalhasConsecutivas())),
                        () -> itens.add(new InicioView.IntegracaoStatusView(
                                "Pluggy", "NAO_CONECTADA", RotulosIntegracao.status("NAO_CONECTADA"), null, null, 0)));
        return itens;
    }

    private List<InicioView.AtividadeView> atividades(UUID empresaId) {
        return jdbc.query("""
                SELECT data, conta_local, descricao_raw, valor_liquido, direcao, estado
                  FROM transacao
                 WHERE empresa_id = ?
                 ORDER BY data DESC, created_at DESC
                 LIMIT 8
                """, this::atividade, empresaId);
    }

    private InicioView.AtividadeView atividade(ResultSet rs, int rowNum) throws SQLException {
        return new InicioView.AtividadeView(
                rs.getDate("data").toLocalDate(),
                rs.getString("conta_local"),
                rs.getString("descricao_raw"),
                rs.getBigDecimal("valor_liquido"),
                rs.getString("direcao"),
                EstadoTransacao.valueOf(rs.getString("estado")).getRotulo());
    }
}
