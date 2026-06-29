package com.planteumaflor.conciliador.transacao.application;

import java.util.UUID;

/** Detecção e desfazer de transferências internas de mesma titularidade. */
public interface TransferenciasInternas {

    /**
     * Varre os lançamentos da empresa e pareia automaticamente as transferências
     * internas inequívocas (mesmo valor e data, direções opostas, contas distintas).
     *
     * @return quantidade de pares criados nesta execução.
     */
    int detectar(UUID empresaId);

    /** Desfaz o pareamento automático de uma transferência interna e de sua perna oposta. */
    void desfazer(UUID empresaId, UUID transacaoId);
}
