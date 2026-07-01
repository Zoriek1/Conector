package com.planteumaflor.conciliador.bling.application;

import java.util.UUID;

/** Conexão OAuth de uma empresa com o Bling (tela 03/07). */
public interface ConectarBling {

    /** Monta a URL de autorização do Bling para o {@code state} já assinado. */
    String urlAutorizacao(String state);

    /** Conclui o callback: troca o {@code code} por tokens e persiste cifrado. */
    void concluir(UUID empresaId, String code);
}
