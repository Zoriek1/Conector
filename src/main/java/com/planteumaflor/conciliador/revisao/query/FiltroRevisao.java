package com.planteumaflor.conciliador.revisao.query;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;

import java.util.Arrays;
import java.util.List;

/** Filtros seguros da fila de revisao. */
public record FiltroRevisao(
        EstadoTransacao estado,
        Direcao direcao,
        ClasseTransacao classe,
        String q
) {
    public List<String> termosBusca() {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return Arrays.stream(q.split("[,\\r\\n]+"))
                .map(String::strip)
                .filter(termo -> !termo.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }
}
