package com.planteumaflor.conciliador.cora.domain;

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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Integração direta de uma empresa com a API do Cora (mTLS + OAuth).
 *
 * Uma por empresa. As credenciais (clientId, certificado, chave privada) são
 * armazenadas cifradas ({@link com.planteumaflor.conciliador.config.CriptoService})
 * e só são decifradas no momento da chamada HTTP.
 */
@Entity
@Table(name = "integracao_cora")
public class IntegracaoCora {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusIntegracaoCora status;

    @Column(name = "client_id_cifrado", nullable = false)
    private String clientIdCifrado;

    @Column(name = "certificado_cifrado", nullable = false)
    private String certificadoCifrado;

    @Column(name = "chave_privada_cifrada", nullable = false)
    private String chavePrivadaCifrada;

    @Column(name = "conta_id_externo")
    private String contaIdExterno;

    @Column(name = "conectado_em", nullable = false)
    private Instant conectadoEm;

    @Column(name = "ultima_sincronizacao")
    private Instant ultimaSincronizacao;

    @Column(name = "ultima_falha_em")
    private Instant ultimaFalhaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "ultima_falha_tipo")
    private TipoFalhaCora ultimaFalhaTipo;

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

    protected IntegracaoCora() {
        // exigido pelo JPA
    }

    public static IntegracaoCora conectada(
            UUID empresaId,
            String clientIdCifrado,
            String certificadoCifrado,
            String chavePrivadaCifrada,
            Instant agora) {
        IntegracaoCora integracao = new IntegracaoCora();
        integracao.empresaId = Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        integracao.atualizarCredenciais(clientIdCifrado, certificadoCifrado, chavePrivadaCifrada, agora);
        return integracao;
    }

    public void atualizarCredenciais(
            String clientIdCifrado,
            String certificadoCifrado,
            String chavePrivadaCifrada,
            Instant agora) {
        this.clientIdCifrado = exigirTexto(clientIdCifrado, "clientIdCifrado");
        this.certificadoCifrado = exigirTexto(certificadoCifrado, "certificadoCifrado");
        this.chavePrivadaCifrada = exigirTexto(chavePrivadaCifrada, "chavePrivadaCifrada");
        this.status = StatusIntegracaoCora.ATIVA;
        this.conectadoEm = Objects.requireNonNull(agora, "agora é obrigatório");
        limparFalhas();
    }

    public void registrarSincronizacao(String contaIdExterno, Instant agora) {
        this.contaIdExterno = contaIdExterno;
        this.ultimaSincronizacao = Objects.requireNonNull(agora, "agora é obrigatório");
        this.status = StatusIntegracaoCora.ATIVA;
        limparFalhas();
    }

    public void registrarFalha(TipoFalhaCora tipo, Instant agora) {
        this.status = StatusIntegracaoCora.REQUER_ATENCAO;
        this.ultimaFalhaTipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
        this.ultimaFalhaEm = Objects.requireNonNull(agora, "agora é obrigatório");
        this.falhasConsecutivas++;
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

    public StatusIntegracaoCora getStatus() {
        return status;
    }

    public String getClientIdCifrado() {
        return clientIdCifrado;
    }

    public String getCertificadoCifrado() {
        return certificadoCifrado;
    }

    public String getChavePrivadaCifrada() {
        return chavePrivadaCifrada;
    }

    public String getContaIdExterno() {
        return contaIdExterno;
    }

    public Instant getConectadoEm() {
        return conectadoEm;
    }

    public Instant getUltimaSincronizacao() {
        return ultimaSincronizacao;
    }

    public Instant getUltimaFalhaEm() {
        return ultimaFalhaEm;
    }

    public TipoFalhaCora getUltimaFalhaTipo() {
        return ultimaFalhaTipo;
    }

    public int getFalhasConsecutivas() {
        return falhasConsecutivas;
    }

    public long getVersion() {
        return version;
    }
}
