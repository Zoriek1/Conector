package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.bling.domain.TipoFalhaBling;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Gera e valida o parâmetro {@code state} do OAuth do Bling (tela 03 §12):
 * assinado (HMAC-SHA256), com expiração e amarrado à empresa. O uso único é
 * garantido no controller, comparando o {@code nonce} com o valor guardado na
 * sessão e descartando-o após o retorno.
 *
 * A chave HMAC é derivada da mesma {@code CRIPTO_KEY} já exigida pelo sistema.
 */
@Component
public class EstadoOAuthBling {

    private static final String HMAC = "HmacSHA256";
    private static final Duration VALIDADE = Duration.ofMinutes(10);
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final ConciliadorProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    EstadoOAuthBling(ConciliadorProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Estado recém-emitido: o {@code valor} vai na URL; o {@code nonce} na sessão. */
    public record Emitido(String valor, String nonce) {
    }

    /** Dados extraídos de um estado válido. */
    public record Conteudo(UUID empresaId, String nonce) {
    }

    public Emitido gerar(UUID empresaId) {
        byte[] nonceBytes = new byte[16];
        random.nextBytes(nonceBytes);
        String nonce = ENC.encodeToString(nonceBytes);
        long exp = clock.instant().plus(VALIDADE).getEpochSecond();
        String payload = empresaId + ":" + exp + ":" + nonce;
        String corpo = ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String valor = corpo + "." + ENC.encodeToString(assinar(corpo));
        return new Emitido(valor, nonce);
    }

    public Conteudo validar(String valor) {
        if (valor == null || !valor.contains(".")) {
            throw new FalhaBlingException(TipoFalhaBling.AUTENTICACAO, "state ausente ou malformado");
        }
        int ponto = valor.indexOf('.');
        String corpo = valor.substring(0, ponto);
        String assinatura = valor.substring(ponto + 1);
        if (!constanteIguais(ENC.encodeToString(assinar(corpo)), assinatura)) {
            throw new FalhaBlingException(TipoFalhaBling.AUTENTICACAO, "state com assinatura inválida");
        }
        String payload = new String(DEC.decode(corpo), StandardCharsets.UTF_8);
        String[] partes = payload.split(":", 3);
        if (partes.length != 3) {
            throw new FalhaBlingException(TipoFalhaBling.AUTENTICACAO, "state com conteúdo inválido");
        }
        long exp = Long.parseLong(partes[1]);
        if (Instant.ofEpochSecond(exp).isBefore(clock.instant())) {
            throw new FalhaBlingException(TipoFalhaBling.AUTENTICACAO, "state expirado");
        }
        return new Conteudo(UUID.fromString(partes[0]), partes[2]);
    }

    private byte[] assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(chave(), HMAC));
            return mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("falha ao assinar state OAuth", e);
        }
    }

    private byte[] chave() {
        String chaveBase64 = properties.cripto() == null ? null : properties.cripto().chave();
        if (chaveBase64 == null || chaveBase64.isBlank()) {
            throw new IllegalStateException(
                    "CRIPTO_KEY não configurada; necessária para assinar o state OAuth do Bling");
        }
        return Base64.getDecoder().decode(chaveBase64);
    }

    private static boolean constanteIguais(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(x, y);
    }
}
