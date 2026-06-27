package com.planteumaflor.conciliador.ofx.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class OfxService {

    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public OfxService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LoteOfxView> listar(UUID empresaId) {
        return jdbc.query("""
                SELECT l.id, c.nome conta, l.data_inicio, l.data_fim, l.status,
                       l.nome_arquivo, l.quantidade_itens, l.total_creditos,
                       l.total_debitos, l.created_at
                  FROM lote_ofx l
                  JOIN conta_bancaria c ON c.id = l.conta_bancaria_id
                 WHERE l.empresa_id = ?
                 ORDER BY l.created_at DESC
                """, this::loteView, empresaId);
    }

    @Transactional
    public UUID gerar(UUID empresaId, UUID contaId, LocalDate inicio, LocalDate fim) {
        ContaOfx conta = carregarConta(empresaId, contaId);
        List<TransacaoOfx> transacoes = carregarTransacoesElegiveis(empresaId, conta, inicio, fim);
        if (transacoes.isEmpty()) {
            throw new IllegalArgumentException("nenhuma transação elegível para OFX");
        }

        UUID loteId = UUID.randomUUID();
        String conteudo = gerarConteudo(loteId, conta, inicio, fim, transacoes);
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        String checksum = sha256(bytes);
        BigDecimal creditos = transacoes.stream()
                .filter(t -> t.direcao().equals("CREDITO"))
                .map(TransacaoOfx::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debitos = transacoes.stream()
                .filter(t -> t.direcao().equals("DEBITO"))
                .map(TransacaoOfx::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String nomeArquivo = "ofx-" + conta.nomeSeguro() + "-" + inicio + "-" + fim + ".ofx";

        jdbc.update("""
                INSERT INTO lote_ofx (
                    id, empresa_id, conta_bancaria_id, data_inicio, data_fim,
                    status, nome_arquivo, media_type, tamanho_bytes,
                    checksum_sha256, conteudo, quantidade_itens,
                    total_creditos, total_debitos
                ) VALUES (?, ?, ?, ?, ?, 'DISPONIVEL', ?, 'application/x-ofx',
                          ?, ?, ?, ?, ?, ?)
                """,
                loteId, empresaId, contaId, inicio, fim, nomeArquivo,
                bytes.length, checksum, bytes, transacoes.size(), creditos, debitos);

        for (TransacaoOfx transacao : transacoes) {
            jdbc.update("INSERT INTO lote_ofx_item (lote_ofx_id, transacao_id) VALUES (?, ?)",
                    loteId, transacao.id());
            jdbc.update("""
                    UPDATE transacao
                       SET ofx_lote_id = ?, updated_at = now()
                     WHERE id = ? AND empresa_id = ?
                    """, loteId.toString(), transacao.id(), empresaId);
        }
        return loteId;
    }

    @Transactional(readOnly = true)
    public ArquivoOfx obterArquivo(UUID empresaId, UUID loteId) {
        return jdbc.query("""
                SELECT nome_arquivo, media_type, conteudo, checksum_sha256
                  FROM lote_ofx
                 WHERE id = ? AND empresa_id = ?
                """, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("lote não encontrado");
            }
            return new ArquivoOfx(
                    rs.getString("nome_arquivo"),
                    rs.getString("media_type"),
                    rs.getBytes("conteudo"),
                    rs.getString("checksum_sha256"));
        }, loteId, empresaId);
    }

    @Transactional
    public void confirmarUpload(UUID empresaId, UUID loteId, String observacao) {
        int alterados = jdbc.update("""
                UPDATE lote_ofx
                   SET status = 'UPLOAD_CONFIRMADO',
                       confirmado_em = ?,
                       observacao = COALESCE(NULLIF(?, ''), observacao),
                       updated_at = now()
                 WHERE id = ? AND empresa_id = ? AND status <> 'UPLOAD_CONFIRMADO'
                """, clock.instant(), observacao, loteId, empresaId);
        if (alterados == 0) {
            boolean existe = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT count(*) > 0 FROM lote_ofx WHERE id = ? AND empresa_id = ?",
                    Boolean.class, loteId, empresaId));
            if (!existe) {
                throw new IllegalArgumentException("lote não encontrado");
            }
        }
        jdbc.update("""
                UPDATE transacao
                   SET estado = 'CONCILIADO', updated_at = now()
                 WHERE empresa_id = ?
                   AND id IN (SELECT transacao_id FROM lote_ofx_item WHERE lote_ofx_id = ?)
                """, empresaId, loteId);
    }

    private ContaOfx carregarConta(UUID empresaId, UUID contaId) {
        return jdbc.query("""
                SELECT id, fonte, id_conta_externa, nome, banco_codigo, agencia, numero, digito
                  FROM conta_bancaria
                 WHERE id = ? AND empresa_id = ?
                """, rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException("conta não encontrada");
            }
            return new ContaOfx(
                    rs.getObject("id", UUID.class),
                    rs.getString("fonte"),
                    rs.getString("id_conta_externa"),
                    rs.getString("nome"),
                    rs.getString("banco_codigo"),
                    rs.getString("agencia"),
                    rs.getString("numero"),
                    rs.getString("digito"));
        }, contaId, empresaId);
    }

    private List<TransacaoOfx> carregarTransacoesElegiveis(
            UUID empresaId, ContaOfx conta, LocalDate inicio, LocalDate fim) {
        return jdbc.query("""
                SELECT id, id_transacao_externa, data, valor_liquido, direcao, descricao_raw
                  FROM transacao
                 WHERE empresa_id = ?
                   AND fonte = ?
                   AND id_conta_externa = ?
                   AND estado = 'EM_LOTE_OFX'
                   AND ofx_lote_id IS NULL
                   AND data BETWEEN ? AND ?
                 ORDER BY data, created_at
                """, this::transacaoOfx, empresaId, conta.fonte(), conta.idContaExterna(), inicio, fim);
    }

    private String gerarConteudo(
            UUID loteId,
            ContaOfx conta,
            LocalDate inicio,
            LocalDate fim,
            List<TransacaoOfx> transacoes) {
        StringBuilder ofx = new StringBuilder();
        ofx.append("OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nSECURITY:NONE\n")
                .append("ENCODING:USASCII\nCHARSET:1252\nCOMPRESSION:NONE\n")
                .append("OLDFILEUID:NONE\nNEWFILEUID:").append(loteId).append("\n\n")
                .append("<OFX>\n<BANKMSGSRSV1>\n<STMTTRNRS>\n<TRNUID>").append(loteId).append("\n")
                .append("<STATUS><CODE>0<SEVERITY>INFO</STATUS>\n<STMTRS>\n<CURDEF>BRL\n")
                .append("<BANKACCTFROM>\n<BANKID>").append(valorOfx(conta.bancoCodigo(), "000"))
                .append("\n<BRANCHID>").append(valorOfx(conta.agencia(), "0000"))
                .append("\n<ACCTID>").append(valorOfx(conta.numeroComDigito(), conta.idContaExterna()))
                .append("\n<ACCTTYPE>CHECKING\n</BANKACCTFROM>\n")
                .append("<BANKTRANLIST>\n<DTSTART>").append(inicio.format(OFX_DATE))
                .append("\n<DTEND>").append(fim.format(OFX_DATE)).append("\n");

        for (TransacaoOfx tx : transacoes) {
            BigDecimal valor = tx.direcao().equals("DEBITO") ? tx.valor().negate() : tx.valor();
            ofx.append("<STMTTRN>\n<TRNTYPE>").append(tx.direcao().equals("DEBITO") ? "DEBIT" : "CREDIT")
                    .append("\n<DTPOSTED>").append(tx.data().format(OFX_DATE))
                    .append("\n<TRNAMT>").append(valor)
                    .append("\n<FITID>").append(valorOfx(tx.idExterno(), tx.id().toString()))
                    .append("\n<MEMO>").append(escapar(tx.descricao()))
                    .append("\n</STMTTRN>\n");
        }
        ofx.append("</BANKTRANLIST>\n</STMTRS>\n</STMTTRNRS>\n</BANKMSGSRSV1>\n</OFX>\n");
        return ofx.toString();
    }

    private String valorOfx(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : escapar(valor);
    }

    private String escapar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "Sem descricao";
        }
        return valor.replace("&", "E").replace("<", "").replace(">", "").strip();
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private LoteOfxView loteView(ResultSet rs, int rowNum) throws SQLException {
        return new LoteOfxView(
                rs.getObject("id", UUID.class),
                rs.getString("conta"),
                rs.getDate("data_inicio").toLocalDate(),
                rs.getDate("data_fim").toLocalDate(),
                rs.getString("status"),
                rs.getString("nome_arquivo"),
                rs.getInt("quantidade_itens"),
                rs.getBigDecimal("total_creditos"),
                rs.getBigDecimal("total_debitos"),
                rs.getTimestamp("created_at").toInstant());
    }

    private TransacaoOfx transacaoOfx(ResultSet rs, int rowNum) throws SQLException {
        return new TransacaoOfx(
                rs.getObject("id", UUID.class),
                rs.getString("id_transacao_externa"),
                rs.getDate("data").toLocalDate(),
                rs.getBigDecimal("valor_liquido"),
                rs.getString("direcao"),
                rs.getString("descricao_raw"));
    }

    public record LoteOfxView(
            UUID id,
            String conta,
            LocalDate inicio,
            LocalDate fim,
            String status,
            String nomeArquivo,
            int quantidadeItens,
            BigDecimal totalCreditos,
            BigDecimal totalDebitos,
            java.time.Instant criadoEm) {}

    public record ArquivoOfx(String nomeArquivo, String mediaType, byte[] conteudo, String checksum) {}

    private record ContaOfx(
            UUID id,
            String fonte,
            String idContaExterna,
            String nome,
            String bancoCodigo,
            String agencia,
            String numero,
            String digito) {
        String numeroComDigito() {
            if (numero == null || numero.isBlank()) {
                return null;
            }
            return digito == null || digito.isBlank() ? numero : numero + "-" + digito;
        }

        String nomeSeguro() {
            return nome == null || nome.isBlank()
                    ? "conta"
                    : nome.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
    }

    private record TransacaoOfx(
            UUID id,
            String idExterno,
            LocalDate data,
            BigDecimal valor,
            String direcao,
            String descricao) {}
}
