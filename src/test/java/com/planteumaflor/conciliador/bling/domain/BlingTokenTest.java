package com.planteumaflor.conciliador.bling.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BlingTokenTest {

    private static final Instant AGORA = Instant.parse("2026-06-30T12:00:00Z");

    @Test
    void conectaAtivoSemFalhas() {
        BlingToken token = novo();

        assertThat(token.getStatus()).isEqualTo(StatusBling.ATIVA);
        assertThat(token.getFalhasConsecutivas()).isZero();
        assertThat(token.getConectadoEm()).isEqualTo(AGORA);
        assertThat(token.desconectado()).isFalse();
    }

    @Test
    void precisaRenovarSomenteDentroDaMargem() {
        BlingToken token = novo(); // expira em AGORA + 1h

        assertThat(token.precisaRenovar(Duration.ofMinutes(2), AGORA)).isFalse();
        assertThat(token.precisaRenovar(Duration.ofMinutes(2), AGORA.plus(Duration.ofMinutes(57)))).isFalse();
        // margem exata (expira - margem): já renova
        assertThat(token.precisaRenovar(Duration.ofMinutes(2), AGORA.plus(Duration.ofMinutes(58)))).isTrue();
        assertThat(token.precisaRenovar(Duration.ofMinutes(2), AGORA.plus(Duration.ofHours(2)))).isTrue();
    }

    @Test
    void atualizarTokensLimpaFalhasERegistraRenovacao() {
        BlingToken token = novo();
        token.registrarFalha(TipoFalhaBling.COMUNICACAO, AGORA);
        assertThat(token.getStatus()).isEqualTo(StatusBling.REQUER_ATENCAO);

        Instant depois = AGORA.plus(Duration.ofMinutes(5));
        token.atualizarTokens("novoAcc", "novoRef", depois.plus(Duration.ofHours(1)), depois);

        assertThat(token.getStatus()).isEqualTo(StatusBling.ATIVA);
        assertThat(token.getFalhasConsecutivas()).isZero();
        assertThat(token.getUltimaFalhaTipo()).isNull();
        assertThat(token.getUltimaRenovacao()).isEqualTo(depois);
    }

    @Test
    void revogacaoEhEstadoTerminal() {
        BlingToken token = novo();
        token.registrarRevogacao(AGORA);

        assertThat(token.getStatus()).isEqualTo(StatusBling.DESCONECTADA);
        assertThat(token.getUltimaFalhaTipo()).isEqualTo(TipoFalhaBling.AUTENTICACAO);
        assertThat(token.desconectado()).isTrue();
    }

    private static BlingToken novo() {
        return BlingToken.conectado(
                UUID.randomUUID(), "acc", "ref", AGORA.plus(Duration.ofHours(1)), AGORA);
    }
}
