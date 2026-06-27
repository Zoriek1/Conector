package com.planteumaflor.conciliador.conta.domain;

import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conta_bancaria")
public class ContaBancaria {

    @Id
    @UuidGenerator
    @Column(name = "id")
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte", nullable = false)
    private FonteIntegracao fonte;

    @Column(name = "id_conta_externa", nullable = false)
    private String idContaExterna;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "banco_codigo")
    private String bancoCodigo;

    @Column(name = "agencia")
    private String agencia;

    @Column(name = "numero")
    private String numero;

    @Column(name = "digito")
    private String digito;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoContaBancaria tipo;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @Column(name = "ultima_sincronizacao")
    private Instant ultimaSincronizacao;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContaBancaria() {
    }

    public static ContaBancaria nova(
            UUID empresaId,
            FonteIntegracao fonte,
            String idContaExterna,
            String nome,
            String bancoCodigo,
            String agencia,
            String numero,
            String digito,
            TipoContaBancaria tipo) {
        ContaBancaria conta = new ContaBancaria();
        conta.empresaId = Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        conta.fonte = Objects.requireNonNull(fonte, "fonte é obrigatória");
        conta.idContaExterna = exigirTexto(idContaExterna, "idContaExterna");
        conta.atualizarDados(nome, bancoCodigo, agencia, numero, digito, tipo);
        conta.ativa = true;
        return conta;
    }

    public void atualizarDados(
            String nome,
            String bancoCodigo,
            String agencia,
            String numero,
            String digito,
            TipoContaBancaria tipo) {
        this.nome = exigirTexto(nome, "nome");
        this.bancoCodigo = textoOpcional(bancoCodigo);
        this.agencia = textoOpcional(agencia);
        this.numero = textoOpcional(numero);
        this.digito = textoOpcional(digito);
        this.tipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
    }

    public void ativar() {
        this.ativa = true;
    }

    public void pausar() {
        this.ativa = false;
    }

    public void registrarSincronizacao(Instant agora) {
        this.ultimaSincronizacao = Objects.requireNonNull(agora, "agora é obrigatório");
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

    public FonteIntegracao getFonte() {
        return fonte;
    }

    public String getIdContaExterna() {
        return idContaExterna;
    }

    public String getNome() {
        return nome;
    }

    public String getBancoCodigo() {
        return bancoCodigo;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getNumero() {
        return numero;
    }

    public String getDigito() {
        return digito;
    }

    public TipoContaBancaria getTipo() {
        return tipo;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getUltimaSincronizacao() {
        return ultimaSincronizacao;
    }
}
