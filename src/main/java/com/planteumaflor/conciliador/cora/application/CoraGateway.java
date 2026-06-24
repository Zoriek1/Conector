package com.planteumaflor.conciliador.cora.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Porta para a API direta do Cora (OAuth client-credentials + mTLS por empresa).
 *
 * O nosso código depende DESTA interface; quem a cumpre hoje é um adapter real
 * que monta um {@code SSLContext} por chamada a partir do certificado/chave da
 * empresa (sem bean estático, porque cada empresa tem seu próprio par).
 */
public interface CoraGateway {

    /** Consulta o saldo da conta; usado também para validar credenciais novas. */
    BigDecimal saldo(CredenciaisCora credenciais);

    /** Consulta o extrato no período [de, ate], inclusive. */
    List<LancamentoExtrato> extrato(CredenciaisCora credenciais, LocalDate de, LocalDate ate);

    record CredenciaisCora(String clientId, String certificadoPem, String chavePrivadaPem) {
    }

    /**
     * Lançamento do extrato, já no vocabulário do nosso domínio.
     *
     * {@code valor} mantém o sinal da API do Cora: positivo é crédito,
     * negativo é débito; quem ingere deriva a {@code Direcao} e normaliza o
     * valor absoluto.
     */
    record LancamentoExtrato(
            String idTransacaoExterna,
            String idContaExterna,
            LocalDate data,
            BigDecimal valor,
            String descricao,
            String contraparteDoc,
            String e2eId) {
    }
}
