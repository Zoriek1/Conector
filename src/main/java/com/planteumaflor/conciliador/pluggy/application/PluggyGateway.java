package com.planteumaflor.conciliador.pluggy.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PluggyGateway {

    String criarApiKey(CredenciaisPluggy credenciais);

    ConnectToken criarConnectToken(String apiKey, String clientUserId);

    List<ContaPluggy> listarContas(String apiKey, String itemId);

    List<TransacaoPluggy> listarTransacoes(String apiKey, String accountId, LocalDate de, LocalDate ate);

    record CredenciaisPluggy(String clientId, String clientSecret) {}

    record ConnectToken(String valor) {}

    record ContaPluggy(
            String id,
            String itemId,
            String nome,
            String numero,
            String tipo,
            String bancoCodigo) {}

    record TransacaoPluggy(
            String id,
            String accountId,
            LocalDate data,
            BigDecimal valor,
            String tipo,
            String descricao,
            String descricaoRaw,
            String providerCode,
            String documentoContraparte) {}
}
