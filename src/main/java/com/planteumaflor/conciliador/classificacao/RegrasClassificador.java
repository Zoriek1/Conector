package com.planteumaflor.conciliador.classificacao;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Confianca;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/** Heurísticas por palavra-chave na descrição e no sinal do movimento. */
@Component
class RegrasClassificador implements Classificador {

    private final BigDecimal confiancaAutomatica;

    RegrasClassificador(ConciliadorProperties properties) {
        this.confiancaAutomatica = properties.classificacao().confiancaAutomatica();
    }

    @Override
    public void classificar(Transacao transacao) {
        Resultado resultado = aplicarRegras(descricaoNormalizada(transacao), transacao.getDirecao());

        if (resultado.confianca().valor().compareTo(confiancaAutomatica) >= 0) {
            transacao.classificar(resultado.classe(), resultado.confianca(), resultado.justificativa());
        } else {
            transacao.enviarParaRevisao("confiança abaixo do limiar automático: " + resultado.justificativa());
        }
    }

    private Resultado aplicarRegras(String descricao, Direcao direcao) {
        if (contemAlguma(descricao, "pro labore", "pró-labore", "prolabore")) {
            return new Resultado(
                    ClasseTransacao.PRO_LABORE,
                    Confianca.de(new BigDecimal("0.950")),
                    "descrição indica pró-labore");
        }
        if (contemAlguma(descricao, "transferencia entre contas", "transferência entre contas", "ted mesma titularidade")) {
            return new Resultado(
                    ClasseTransacao.TRANSFERENCIA_INTERNA,
                    Confianca.de(new BigDecimal("0.950")),
                    "descrição indica transferência entre contas próprias");
        }
        if (direcao == Direcao.CREDITO
                && contemAlguma(descricao, "pix recebido", "boleto recebido", "venda", "recebimento")) {
            return new Resultado(
                    ClasseTransacao.CREDITO_VENDA,
                    Confianca.de(new BigDecimal("0.920")),
                    "crédito com descrição de recebimento/venda");
        }
        if (direcao == Direcao.DEBITO
                && contemAlguma(descricao, "taxa", "tarifa", "folha de pagamento", "salario", "salário")) {
            return new Resultado(
                    ClasseTransacao.DEBITO_DESPESA,
                    Confianca.de(new BigDecimal("0.920")),
                    "débito com descrição de taxa/folha");
        }
        return new Resultado(ClasseTransacao.INDEFINIDO, Confianca.zero(), "nenhuma regra correspondeu");
    }

    private boolean contemAlguma(String descricao, String... termos) {
        for (String termo : termos) {
            if (descricao.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private String descricaoNormalizada(Transacao transacao) {
        String raw = transacao.getDescricaoRaw();
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT);
    }

    private record Resultado(ClasseTransacao classe, Confianca confianca, String justificativa) {}
}
