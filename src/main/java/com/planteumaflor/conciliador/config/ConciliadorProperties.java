package com.planteumaflor.conciliador.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuração própria do Conciliador, validada no startup (Backend §12).
 *
 * Usa {@code record} (DTO imutável). As restrições Bean Validation fazem a
 * aplicação falhar logo na subida se algum valor essencial estiver ausente ou
 * fora da faixa — melhor descobrir no boot do que em produção.
 *
 * As credenciais sensíveis (bling.clientSecret, cripto.chave) NÃO são marcadas
 * como obrigatórias ainda: no esqueleto nenhum bean as consome. Tornar-se-ão
 * obrigatórias quando as integrações forem implementadas.
 */
@Validated
@ConfigurationProperties(prefix = "conciliador")
public record ConciliadorProperties(
        @NotBlank String timezone,
        @NotNull @Valid Ingest ingest,
        @NotNull @Valid Classificacao classificacao,
        @Valid Bling bling,
        @Valid Pluggy pluggy,
        @Valid Cora cora,
        @Valid Cripto cripto
) {

    public record Ingest(
            @NotBlank String cron,
            @Positive int diasRetroativos,
            @Positive int maxTentativas,
            @NotNull Duration atrasoRetry
    ) {}

    public record Classificacao(
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confiancaAutomatica,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal toleranciaTaxa
    ) {}

    public record Bling(
            String baseUrl,
            String authorizeUrl,
            String tokenUrl,
            String redirectUri,
            String clientId,
            String clientSecret,
            Duration margemExpiracao
    ) {}

    public record Pluggy(
            String baseUrl
    ) {}

    /**
     * Endpoints da "Integração Direta" do Cora (OAuth client-credentials + mTLS
     * por empresa). Os paths abaixo são o melhor entendimento dos docs públicos
     * e PRECISAM ser confirmados contra o sandbox real do Cora antes de uso em
     * produção — ver risco documentado no plano de integração.
     */
    public record Cora(
            String authBaseUrl,
            String apiBaseUrl,
            String tokenPath,
            String extratoPath,
            String saldoPath
    ) {}

    public record Cripto(
            String chave
    ) {}
}
