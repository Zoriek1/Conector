package com.planteumaflor.conciliador.config;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra/decifra segredos (credenciais Cora, refresh token Bling) com AES-GCM.
 *
 * A chave ({@code CRIPTO_KEY}) é lida sob demanda, não no boot: no esqueleto
 * nem todo ambiente precisa dela, então só falha quando alguém tenta cifrar
 * ou decifrar de fato.
 */
@Service
public class CriptoService {

    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final ConciliadorProperties properties;
    private final SecureRandom random = new SecureRandom();

    public CriptoService(ConciliadorProperties properties) {
        this.properties = properties;
    }

    public String cifrar(String textoPlano) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            byte[] iv = new byte[TAMANHO_IV_BYTES];
            random.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, chave(), new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cifrado.length);
            buffer.put(iv).put(cifrado);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("falha ao cifrar segredo", e);
        }
    }

    public String decifrar(String textoCifrado) {
        try {
            byte[] dados = Base64.getDecoder().decode(textoCifrado);
            ByteBuffer buffer = ByteBuffer.wrap(dados);
            byte[] iv = new byte[TAMANHO_IV_BYTES];
            buffer.get(iv);
            byte[] cifrado = new byte[buffer.remaining()];
            buffer.get(cifrado);

            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.DECRYPT_MODE, chave(), new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] textoPlano = cipher.doFinal(cifrado);
            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("falha ao decifrar segredo", e);
        }
    }

    private SecretKeySpec chave() {
        String chaveBase64 = properties.cripto() == null ? null : properties.cripto().chave();
        if (chaveBase64 == null || chaveBase64.isBlank()) {
            throw new IllegalStateException(
                    "CRIPTO_KEY não configurada; necessária para cifrar/decifrar segredos");
        }
        byte[] bytes = Base64.getDecoder().decode(chaveBase64);
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new IllegalStateException(
                    "CRIPTO_KEY inválida: deve decodificar para 16, 24 ou 32 bytes (AES-128/192/256)");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
