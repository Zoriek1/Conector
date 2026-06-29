package com.planteumaflor.conciliador.revisao.application;

import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comandos da revisão humana (Backend §6.6). Cada operação revalida o estado do
 * agregado e protege contra edição concorrente comparando a versão enviada pelo
 * formulário com a persistida.
 */
public interface RevisarTransacao {

    void aprovar(UUID empresaId, UUID transacaoId, long versaoEsperada);

    void classificar(UUID empresaId, UUID transacaoId, long versaoEsperada, ClasseTransacao classe);

    void selecionarMatch(
            UUID empresaId,
            UUID transacaoId,
            long versaoEsperada,
            String tipo,
            String idExterno,
            BigDecimal taxaDerivada);

    void rotearParaOfx(UUID empresaId, UUID transacaoId, long versaoEsperada);

    void solicitarRetry(UUID empresaId, UUID transacaoId, long versaoEsperada);
}
