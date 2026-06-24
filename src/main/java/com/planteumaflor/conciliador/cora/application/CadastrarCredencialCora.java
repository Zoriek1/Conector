package com.planteumaflor.conciliador.cora.application;

import java.util.UUID;

/**
 * Caso de uso: cadastrar (ou atualizar) a credencial da integração direta com
 * o Cora de uma empresa.
 */
public interface CadastrarCredencialCora {

    void cadastrar(UUID empresaId, String clientId, String certificadoPem, String chavePrivadaPem);
}
