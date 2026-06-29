package com.planteumaflor.conciliador.cora.application;

import java.util.UUID;

/** Caso de uso: puxar o extrato do Cora e ingerir as transações novas. */
public interface SincronizarExtratoCora {

    void sincronizar(UUID empresaId);
}
