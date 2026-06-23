package com.planteumaflor.conciliador.transacao.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Dados canônicos produzidos na borda de ingestão para criar uma transação. */
public record DadosTransacao(
        UUID empresaId,
        String pluggyTransactionId,
        String pluggyAccountId,
        String contaLocal,
        LocalDate data,
        BigDecimal valorLiquido,
        Direcao direcao,
        String descricaoRaw,
        String contraparteDoc,
        String e2eId
) {
    public DadosTransacao {
        Objects.requireNonNull(empresaId, "empresaId é obrigatório");
        Objects.requireNonNull(data, "data é obrigatória");
        Objects.requireNonNull(valorLiquido, "valorLiquido é obrigatório");
        Objects.requireNonNull(direcao, "direcao é obrigatória");
    }
}
