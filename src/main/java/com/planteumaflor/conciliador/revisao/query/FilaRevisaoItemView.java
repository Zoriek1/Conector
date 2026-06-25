package com.planteumaflor.conciliador.revisao.query;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Projeção segura da fila; nenhuma entidade JPA chega ao template. */
public record FilaRevisaoItemView(
        UUID id,
        long version,
        LocalDate data,
        String conta,
        String descricao,
        Direcao direcao,
        BigDecimal valorLiquido,
        ClasseTransacao classeSugerida,
        BigDecimal confianca,
        String motivo
) {}
