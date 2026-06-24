package com.planteumaflor.conciliador.pluggy.application;

import java.util.UUID;

/**
 * Caso de uso: conectar o Meu Pluggy da empresa.
 *
 * Fronteira que o controller usa, sem conhecer a porta nem o repositório.
 */
public interface ConectarPluggy {

    void conectar(UUID empresaId);
}
