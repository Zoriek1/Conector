package com.planteumaflor.conciliador.pluggy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Conexão Pluggy de uma empresa (tela 03/07).
 *
 * Criada já ATIVA quando a conexão conclui (no fake, o adapter chama
 * {@link #conectada}). A existência de uma integração ATIVA é o que faz o
 * onboarding avançar de "Pluggy pendente" para "concluído".
 */
@Entity
@Table(name = "integracao_pluggy")
public class IntegracaoPluggy {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusIntegracao status;

    @Column(name = "pluggy_item_id")
    private String pluggyItemId;

    @Column(name = "client_id_cifrado")
    private String clientIdCifrado;

    @Column(name = "client_secret_cifrado")
    private String clientSecretCifrado;

    @Column(name = "conectado_em")
    private Instant conectadoEm;

    @Column(name = "ultima_sincronizacao")
    private Instant ultimaSincronizacao;

    @Column(name = "ultima_falha_em")
    private Instant ultimaFalhaEm;

    @Column(name = "ultima_falha_tipo")
    private String ultimaFalhaTipo;

    @Column(name = "falhas_consecutivas", nullable = false)
    private int falhasConsecutivas;

    @Version
    @Column(name = "version")
    private long version;

    protected IntegracaoPluggy() {
        // exigido pelo JPA
    }

    /** Cria uma conexão já ATIVA (conclusão do fluxo Pluggy). */
    public static IntegracaoPluggy conectada(UUID empresaId, String pluggyItemId) {
        IntegracaoPluggy integracao = new IntegracaoPluggy();
        integracao.empresaId = Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        integracao.confirmarItem(pluggyItemId, Instant.now());
        return integracao;
    }

    public static IntegracaoPluggy comCredenciais(
            UUID empresaId,
            String clientIdCifrado,
            String clientSecretCifrado,
            Instant agora) {
        IntegracaoPluggy integracao = new IntegracaoPluggy();
        integracao.empresaId = Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        integracao.atualizarCredenciais(clientIdCifrado, clientSecretCifrado, agora);
        return integracao;
    }

    public void atualizarCredenciais(String clientIdCifrado, String clientSecretCifrado, Instant agora) {
        this.clientIdCifrado = exigirTexto(clientIdCifrado, "clientIdCifrado");
        this.clientSecretCifrado = exigirTexto(clientSecretCifrado, "clientSecretCifrado");
        if (this.pluggyItemId == null) {
            this.status = StatusIntegracao.NAO_CONECTADA;
        }
        this.conectadoEm = Objects.requireNonNull(agora, "agora é obrigatório");
        limparFalhas();
    }

    public void confirmarItem(String pluggyItemId, Instant agora) {
        this.pluggyItemId = exigirTexto(pluggyItemId, "pluggyItemId");
        this.status = StatusIntegracao.ATIVA;
        this.conectadoEm = Objects.requireNonNull(agora, "agora é obrigatório");
        limparFalhas();
    }

    public void registrarSincronizacao(Instant agora) {
        this.ultimaSincronizacao = Objects.requireNonNull(agora, "agora é obrigatório");
        this.status = StatusIntegracao.ATIVA;
        limparFalhas();
    }

    public void registrarFalha(String tipo, Instant agora) {
        this.status = StatusIntegracao.REQUER_ATENCAO;
        this.ultimaFalhaTipo = exigirTexto(tipo, "tipo");
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
        return valor.strip();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public StatusIntegracao getStatus() {
        return status;
    }

    public String getPluggyItemId() {
        return pluggyItemId;
    }

    public String getClientIdCifrado() {
        return clientIdCifrado;
    }

    public String getClientSecretCifrado() {
        return clientSecretCifrado;
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

    public String getUltimaFalhaTipo() {
        return ultimaFalhaTipo;
    }

    public int getFalhasConsecutivas() {
        return falhasConsecutivas;
    }
}
