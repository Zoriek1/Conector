package com.planteumaflor.conciliador.integracoes;

/**
 * Rótulos amigáveis para os enums de integração, para que a UI nunca exiba os
 * nomes técnicos (ATIVA, REQUER_ATENCAO, AUTENTICACAO, ...).
 */
public final class RotulosIntegracao {

    private RotulosIntegracao() {
    }

    public static String status(String codigo) {
        if (codigo == null) {
            return "Não conectada";
        }
        return switch (codigo) {
            case "ATIVA" -> "Conectada";
            case "REQUER_ATENCAO" -> "Requer atenção";
            case "DESCONECTADA" -> "Desconectada";
            default -> "Não conectada";
        };
    }

    public static String falha(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "AUTENTICACAO" -> "Falha de autenticação nas credenciais";
            case "COMUNICACAO" -> "Falha de comunicação com o banco";
            case "DADOS_INVALIDOS" -> "Dados inválidos recebidos";
            default -> "Erro interno ao sincronizar";
        };
    }
}
