package com.planteumaflor.conciliador.bling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token OAuth 2.0 de uma empresa com o Bling (Backend §9.2, Bling-API-v3 §6).
 *
 * Um por empresa. {@code accessTokenCifrado} e {@code refreshTokenCifrado} são
 * cifrados ({@link com.planteumaflor.conciliador.config.CriptoService}) e só são
 * decifrados no momento da chamada HTTP. O refresh é serializado com lock
 * pessimista na linha (ver repositório) para nunca renovar em paralelo.
 */
@Entity
@Table(name = "bling_oauth_token")
public class BlingToken {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusBling status;

    @Column(name = "access_token_cifrado", nullable = false)
    private String accessTokenCifrado;

    @Column(name = "refresh_token_cifrado", nullable = false)
    private String refreshTokenCifrado;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "conectado_em", nullable = false)
    private Instant conectadoEm;

    @Column(name = "ultima_renovacao")
    private Instant ultimaRenovacao;

    @Column(name = "ultima_falha_em")
    private Instant ultimaFalhaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "ultima_falha_tipo")
    private TipoFalhaBling ultimaFalhaTipo;

    @Column(name = "falhas_consecutivas", nullable = false)
    private int falhasConsecutivas;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BlingToken() {
        // exigido pelo JPA
    }

    public static BlingToken conectado(
            UUID empresaId,
            String accessTokenCifrado,
            String refreshTokenCifrado,
            Instant expiraEm,
            Instant agora) {
        BlingToken token = new BlingToken();
        token.empresaId = Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        token.conectadoEm = Objects.requireNonNull(agora, "agora é obrigatório");
        token.aplicarTokens(accessTokenCifrado, refreshTokenCifrado, expiraEm, agora);
        return token;
    }

    /** Substitui os tokens após nova autorização ou renovação bem-sucedida. */
    public void atualizarTokens(
            String accessTokenCifrado,
            String refreshTokenCifrado,
            Instant expiraEm,
            Instant agora) {
        aplicarTokens(accessTokenCifrado, refreshTokenCifrado, expiraEm, agora);
        this.ultimaRenovacao = agora;
    }

    private void aplicarTokens(
            String accessTokenCifrado,
            String refreshTokenCifrado,
            Instant expiraEm,
            Instant agora) {
        this.accessTokenCifrado = exigirTexto(accessTokenCifrado, "accessTokenCifrado");
        this.refreshTokenCifrado = exigirTexto(refreshTokenCifrado, "refreshTokenCifrado");
        this.expiraEm = Objects.requireNonNull(expiraEm, "expiraEm é obrigatório");
        this.status = StatusBling.ATIVA;
        limparFalhas();
    }

    /** Falha transitória (rede, 5xx, dado inesperado): mantém os tokens. */
    public void registrarFalha(TipoFalhaBling tipo, Instant agora) {
        this.status = StatusBling.REQUER_ATENCAO;
        this.ultimaFalhaTipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
        this.ultimaFalhaEm = Objects.requireNonNull(agora, "agora é obrigatório");
        this.falhasConsecutivas++;
    }

    /**
     * Refresh token revogado pelo Bling: estado terminal. As escritas param até
     * a empresa reconectar; a saúde fica clara no Actuator (Backend §9.2).
     */
    public void registrarRevogacao(Instant agora) {
        this.status = StatusBling.DESCONECTADA;
        this.ultimaFalhaTipo = TipoFalhaBling.AUTENTICACAO;
        this.ultimaFalhaEm = Objects.requireNonNull(agora, "agora é obrigatório");
        this.falhasConsecutivas++;
    }

    /** Considera expirado um pouco antes do prazo real (margem configurável). */
    public boolean precisaRenovar(Duration margem, Instant agora) {
        Objects.requireNonNull(margem, "margem é obrigatória");
        Objects.requireNonNull(agora, "agora é obrigatório");
        return !agora.plus(margem).isBefore(expiraEm);
    }

    public boolean desconectado() {
        return status == StatusBling.DESCONECTADA;
    }

    private void limparFalhas() {
        this.ultimaFalhaTipo = null;
        this.ultimaFalhaEm = null;
        this.falhasConsecutivas = 0;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public StatusBling getStatus() {
        return status;
    }

    public String getAccessTokenCifrado() {
        return accessTokenCifrado;
    }

    public String getRefreshTokenCifrado() {
        return refreshTokenCifrado;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getConectadoEm() {
        return conectadoEm;
    }

    public Instant getUltimaRenovacao() {
        return ultimaRenovacao;
    }

    public Instant getUltimaFalhaEm() {
        return ultimaFalhaEm;
    }

    public TipoFalhaBling getUltimaFalhaTipo() {
        return ultimaFalhaTipo;
    }

    public int getFalhasConsecutivas() {
        return falhasConsecutivas;
    }

    public long getVersion() {
        return version;
    }
}
