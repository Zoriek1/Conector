package com.planteumaflor.conciliador.transacao.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Raiz do agregado financeiro e fonte de verdade do estado de conciliação. */
@Entity
@Table(
        name = "transacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_transacao_pluggy",
                columnNames = {"empresa_id", "pluggy_transaction_id"}))
public class Transacao {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "pluggy_transaction_id", nullable = false)
    private String pluggyTransactionId;

    @Column(name = "pluggy_account_id", nullable = false)
    private String pluggyAccountId;

    @Column(name = "conta_local", nullable = false)
    private String contaLocal;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "valor_liquido", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(name = "direcao", nullable = false)
    private Direcao direcao;

    @Column(name = "descricao_raw")
    private String descricaoRaw;

    @Column(name = "contraparte_doc")
    private String contraparteDoc;

    @Column(name = "e2e_id")
    private String e2eId;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe", nullable = false)
    private ClasseTransacao classe;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(
            name = "confianca", nullable = false, precision = 4, scale = 3))
    private Confianca confianca;

    @Column(name = "justificativa_classificacao")
    private String justificativaClassificacao;

    @Column(name = "motivo_revisao")
    private String motivoRevisao;

    @Column(name = "match_bling_tipo")
    private String matchBlingTipo;

    @Column(name = "match_bling_id")
    private String matchBlingId;

    @Column(name = "taxa_derivada", precision = 14, scale = 2)
    private BigDecimal taxaDerivada;

    @Column(name = "transfer_par_id")
    private UUID transferParId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTransacao estado;

    @Column(name = "bling_bordero_id")
    private String blingBorderoId;

    @Column(name = "ofx_lote_id")
    private String ofxLoteId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transacao() {
        // exigido pelo JPA
    }

    public static Transacao ingerida(DadosTransacao dados) {
        Objects.requireNonNull(dados, "dados da transação são obrigatórios");

        Transacao transacao = new Transacao();
        transacao.empresaId = dados.empresaId();
        transacao.pluggyTransactionId = exigirTexto(
                dados.pluggyTransactionId(), "pluggyTransactionId");
        transacao.pluggyAccountId = exigirTexto(
                dados.pluggyAccountId(), "pluggyAccountId");
        transacao.contaLocal = exigirTexto(dados.contaLocal(), "contaLocal");
        transacao.data = dados.data();
        transacao.valorLiquido = normalizarDinheiroPositivo(
                dados.valorLiquido(), "valorLiquido");
        transacao.direcao = dados.direcao();
        transacao.descricaoRaw = textoOpcional(dados.descricaoRaw());
        transacao.contraparteDoc = textoOpcional(dados.contraparteDoc());
        transacao.e2eId = textoOpcional(dados.e2eId());
        transacao.classe = ClasseTransacao.INDEFINIDO;
        transacao.confianca = Confianca.zero();
        transacao.estado = EstadoTransacao.INGERIDO;
        return transacao;
    }

    public void classificar(ClasseTransacao classe, Confianca confianca, String justificativa) {
        exigirEstado(EstadoTransacao.INGERIDO);
        this.classe = Objects.requireNonNull(classe, "classe é obrigatória");
        this.confianca = Objects.requireNonNull(confianca, "confiança é obrigatória");
        this.justificativaClassificacao = exigirTexto(justificativa, "justificativa");
        this.motivoRevisao = null;
        this.estado = EstadoTransacao.CLASSIFICADO;
    }

    public void enviarParaRevisao(String motivo) {
        exigirEstado(EstadoTransacao.INGERIDO, EstadoTransacao.CLASSIFICADO);
        this.motivoRevisao = exigirTexto(motivo, "motivo da revisão");
        this.estado = EstadoTransacao.EM_REVISAO;
    }

    public void registrarMatch(String tipo, String idExterno, BigDecimal taxaDerivada) {
        exigirEstado(EstadoTransacao.CLASSIFICADO, EstadoTransacao.EM_REVISAO);
        String tipoNormalizado = exigirTexto(tipo, "tipo do match").toUpperCase(Locale.ROOT);
        if (!tipoNormalizado.equals("CONTA_RECEBER") && !tipoNormalizado.equals("CONTA_PAGAR")) {
            throw new IllegalArgumentException("tipo do match inválido");
        }
        this.matchBlingTipo = tipoNormalizado;
        this.matchBlingId = exigirTexto(idExterno, "id externo do match");
        this.taxaDerivada = taxaDerivada == null
                ? null
                : normalizarDinheiroNaoNegativo(taxaDerivada, "taxaDerivada");
    }

    public void removerMatch() {
        exigirEstado(EstadoTransacao.CLASSIFICADO, EstadoTransacao.EM_REVISAO);
        this.matchBlingTipo = null;
        this.matchBlingId = null;
        this.taxaDerivada = null;
    }

    public void parearTransferencia(UUID outraTransacaoId) {
        exigirEstado(EstadoTransacao.CLASSIFICADO, EstadoTransacao.EM_REVISAO);
        if (classe != ClasseTransacao.TRANSFERENCIA_INTERNA) {
            throw new IllegalStateException("somente transferência interna pode ser pareada");
        }
        UUID par = Objects.requireNonNull(outraTransacaoId, "transação par é obrigatória");
        if (par.equals(id)) {
            throw new IllegalArgumentException("transação não pode ser pareada consigo mesma");
        }
        this.transferParId = par;
    }

    public void aprovarParaApi() {
        exigirEstado(EstadoTransacao.CLASSIFICADO, EstadoTransacao.EM_REVISAO);
        this.estado = EstadoTransacao.AGUARDANDO_ESCRITA_API;
    }

    public void rotearParaOfx() {
        exigirEstado(EstadoTransacao.CLASSIFICADO, EstadoTransacao.EM_REVISAO);
        this.estado = EstadoTransacao.EM_LOTE_OFX;
    }

    public void associarLoteOfx(String loteId) {
        exigirEstado(EstadoTransacao.EM_LOTE_OFX);
        this.ofxLoteId = exigirTexto(loteId, "lote OFX");
    }

    public void registrarEscrita(String blingBorderoId) {
        exigirEstado(EstadoTransacao.AGUARDANDO_ESCRITA_API);
        this.blingBorderoId = exigirTexto(blingBorderoId, "borderô Bling");
        this.estado = EstadoTransacao.ESCRITO_API;
    }

    public void registrarFalha() {
        exigirEstado(EstadoTransacao.AGUARDANDO_ESCRITA_API);
        this.estado = EstadoTransacao.FALHA;
    }

    public void tentarNovamenteApi() {
        exigirEstado(EstadoTransacao.FALHA);
        this.estado = EstadoTransacao.AGUARDANDO_ESCRITA_API;
    }

    public void conciliar() {
        exigirEstado(EstadoTransacao.ESCRITO_API, EstadoTransacao.EM_LOTE_OFX);
        this.estado = EstadoTransacao.CONCILIADO;
    }

    private void exigirEstado(EstadoTransacao... permitidos) {
        if (Arrays.stream(permitidos).noneMatch(estado::equals)) {
            throw new IllegalStateException(
                    "transição não permitida a partir de " + estado);
        }
    }

    private static BigDecimal normalizarDinheiroPositivo(BigDecimal valor, String campo) {
        BigDecimal normalizado = normalizarDinheiro(valor, campo);
        if (normalizado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(campo + " deve ser positivo");
        }
        return normalizado;
    }

    private static BigDecimal normalizarDinheiroNaoNegativo(BigDecimal valor, String campo) {
        BigDecimal normalizado = normalizarDinheiro(valor, campo);
        if (normalizado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return normalizado;
    }

    private static BigDecimal normalizarDinheiro(BigDecimal valor, String campo) {
        return Objects.requireNonNull(valor, campo + " é obrigatório")
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor.strip();
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getPluggyTransactionId() {
        return pluggyTransactionId;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }

    public Direcao getDirecao() {
        return direcao;
    }

    public ClasseTransacao getClasse() {
        return classe;
    }

    public Confianca getConfianca() {
        return confianca;
    }

    public EstadoTransacao getEstado() {
        return estado;
    }

    public String getMotivoRevisao() {
        return motivoRevisao;
    }

    public String getBlingBorderoId() {
        return blingBorderoId;
    }

    public long getVersion() {
        return version;
    }
}
