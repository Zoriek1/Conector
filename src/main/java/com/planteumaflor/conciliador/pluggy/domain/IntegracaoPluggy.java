package com.planteumaflor.conciliador.pluggy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

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
        integracao.pluggyItemId = Objects.requireNonNull(pluggyItemId, "pluggyItemId é obrigatório");
        integracao.status = StatusIntegracao.ATIVA;
        return integracao;
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
}
