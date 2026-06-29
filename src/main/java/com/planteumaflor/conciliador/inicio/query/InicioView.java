package com.planteumaflor.conciliador.inicio.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InicioView(
        long emRevisao,
        long falhas,
        long aguardandoApi,
        long aguardandoOfx,
        long conciliadas,
        List<IntegracaoStatusView> integracoes,
        List<AtividadeView> atividades,
        Instant atualizadoEm) {

    public record IntegracaoStatusView(
            String nome,
            String status,
            Instant ultimaSincronizacao,
            String ultimaFalha,
            int falhasConsecutivas) {}

    public record AtividadeView(
            LocalDate data,
            String conta,
            String descricao,
            BigDecimal valor,
            String direcao,
            String estado) {}
}
