package com.planteumaflor.conciliador.revisao.application;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ClassificarLoteCommand(
        List<ItemVersao> itens,
        ClasseTransacao classe,
        String justificativa
) {
    public ClassificarLoteCommand {
        itens = List.copyOf(Objects.requireNonNull(itens, "itens sao obrigatorios"));
        Objects.requireNonNull(classe, "classe e obrigatoria");
    }

    public record ItemVersao(UUID transacaoId, long version) {
        public ItemVersao {
            Objects.requireNonNull(transacaoId, "transacaoId e obrigatorio");
        }
    }
}
