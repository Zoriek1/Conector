package com.planteumaflor.conciliador.classificacao;

import com.planteumaflor.conciliador.transacao.domain.Transacao;

/**
 * Classifica uma transação recém-ingerida, levando-a a CLASSIFICADO (confiança
 * suficiente) ou EM_REVISAO (confiança baixa), conforme
 * {@code conciliador.classificacao.confianca-automatica}.
 *
 * Escopo deliberadamente pequeno: heurísticas por palavra-chave, não o motor de
 * classificação completo do roadmap. Pode ser substituído sem mudar esta porta.
 */
public interface Classificador {

    void classificar(Transacao transacao);
}
