package com.planteumaflor.conciliador.classificacao;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Confianca;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/** Heuristicas por palavra-chave na descricao e no sinal do movimento. */
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
            transacao.enviarParaRevisao("confianca abaixo do limiar automatico: " + resultado.justificativa());
        }
    }

    private Resultado aplicarRegras(String descricao, Direcao direcao) {
        if (contemAlguma(descricao,
                "transferencia entre contas",
                "transferencia mesma titularidade",
                "ted mesma titularidade",
                "pix entre contas",
                "mesma titularidade",
                "conta reserva")) {
            return resultado(
                    ClasseTransacao.TRANSFERENCIA_INTERNA,
                    "descricao indica transferencia entre contas proprias");
        }

        if (direcao == Direcao.CREDITO) {
            return classificarCredito(descricao);
        }
        return classificarDebito(descricao);
    }

    private Resultado classificarCredito(String descricao) {
        if (contemAlguma(descricao,
                "pix recebido", "boleto recebido", "boleto liquidado",
                "venda", "recebimento de cliente", "cartao recebido",
                "marketplace", "link de pagamento", "pedido recebido")) {
            return resultado(
                    ClasseTransacao.CREDITO_VENDA,
                    "credito com descricao de recebimento/venda");
        }
        if (contemAlguma(descricao,
                "rendimento", "rendimento bancario", "juros recebidos",
                "remuneracao", "cashback", "desconto financeiro recebido")) {
            return resultado(
                    ClasseTransacao.RECEITA_FINANCEIRA,
                    "credito com descricao de receita financeira");
        }
        if (contemAlguma(descricao,
                "estorno", "reembolso", "devolucao recebida",
                "chargeback revertido", "compra cancelada")) {
            return resultado(
                    ClasseTransacao.ESTORNO_REEMBOLSO_RECEBIDO,
                    "credito com descricao de estorno ou reembolso");
        }
        if (contemAlguma(descricao,
                "aporte socio", "aporte de socio", "aporte de capital",
                "capital social", "socio colocou", "socio transferiu")) {
            return resultado(
                    ClasseTransacao.APORTE_SOCIO,
                    "credito com descricao de aporte de socio");
        }
        if (contemAlguma(descricao,
                "emprestimo recebido", "credito tomado", "mutuo recebido",
                "dinheiro emprestado", "antecipacao emprestimo")) {
            return resultado(
                    ClasseTransacao.EMPRESTIMO_RECEBIDO,
                    "credito com descricao de emprestimo recebido");
        }
        return indefinido();
    }

    private Resultado classificarDebito(String descricao) {
        if (contemAlguma(descricao,
                "venda cancelada", "cancelamento de venda", "pedido cancelado",
                "estorno venda", "estorno de venda", "estorno cliente",
                "reembolso cliente", "reembolso ao cliente", "devolucao cliente",
                "chargeback", "charge back")) {
            return resultado(
                    ClasseTransacao.VENDA_CANCELADA_ESTORNO,
                    "debito com descricao de venda cancelada ou estorno ao cliente");
        }
        if (contemAlguma(descricao, "pro labore", "prolabore", "inss pro labore")) {
            return resultado(ClasseTransacao.PRO_LABORE, "descricao indica pro-labore");
        }
        if (contemAlguma(descricao,
                "distribuicao de lucros", "retirada socio", "retirada de socio",
                "retirada extra", "dividendos", "lucros distribuidos")) {
            return resultado(
                    ClasseTransacao.RETIRADA_DISTRIBUICAO_SOCIOS,
                    "descricao indica retirada ou distribuicao de socios");
        }
        if (contemAlguma(descricao,
                "rosas", "rosa ", "lirios", "lirio", "girassol", "astromelia",
                "folhagem", "folhagens", "flores", "flor natural", "plantas para venda",
                "planta para venda", "mudas para venda", "muda para venda")) {
            return resultado(
                    ClasseTransacao.FLORES_FOLHAGENS_PLANTAS,
                    "debito com descricao de flores, folhagens ou plantas");
        }
        if (contemAlguma(descricao,
                "papel para buque", "celofane", "lacos", "laco", "fitas", "fita",
                "adesivo", "cartao", "cachepo", "vaso", "urso", "chocolate",
                "balao", "espuma floral", "embalagem")) {
            return resultado(
                    ClasseTransacao.EMBALAGENS_COMPLEMENTOS,
                    "debito com descricao de embalagem ou complemento do produto");
        }
        if (contemAlguma(descricao,
                "terra", "substrato", "adubo", "fertilizante", "casca de pinus",
                "pedra decorativa", "grama", "manta", "jardinagem", "paisagismo")) {
            return resultado(
                    ClasseTransacao.INSUMOS_JARDINAGEM,
                    "debito com descricao de insumo para jardinagem");
        }
        if (contemAlguma(descricao,
                "gasolina", "combustivel", "etanol", "alcool combustivel",
                "diesel", "posto de gasolina", "posto combustivel", "abastecimento")) {
            return resultado(
                    ClasseTransacao.COMBUSTIVEL_VEICULO,
                    "debito com descricao de gasolina ou combustivel");
        }
        if (contemAlguma(descricao,
                "manutencao veiculo", "manutencao de veiculo", "oficina",
                "revisao carro", "revisao veiculo", "troca de oleo",
                "pneu", "pneus", "mecanico", "auto pecas", "autopecas")) {
            return resultado(
                    ClasseTransacao.MANUTENCAO_VEICULO,
                    "debito com descricao de manutencao de veiculo");
        }
        if (contemAlguma(descricao,
                "pedagio", "estacionamento", "zona azul", "rotativo")) {
            return resultado(
                    ClasseTransacao.PEDAGIO_ESTACIONAMENTO,
                    "debito com descricao de pedagio ou estacionamento");
        }
        if (contemAlguma(descricao,
                "motoboy", "frete", "entrega", "entregas", "correios",
                "transportadora", "logistica", "envio")) {
            return resultado(
                    ClasseTransacao.FRETE_ENTREGAS,
                    "debito com descricao de frete ou entrega");
        }
        if (contemAlguma(descricao,
                "fornecedor", "pagamento fornecedor", "compra mercadoria",
                "produto para revenda", "material para pedido", "servico terceirizado pedido")) {
            return resultado(
                    ClasseTransacao.OUTROS_CUSTOS_DIRETOS,
                    "debito com descricao de custo direto da venda");
        }
        if (contemAlguma(descricao,
                "marketing", "trafego pago", "facebook ads", "google ads",
                "meta ads", "instagram ads", "publicidade", "propaganda",
                "impulsionamento", "designer de criativos", "foto produto", "video produto")) {
            return resultado(
                    ClasseTransacao.MARKETING_TRAFEGO,
                    "debito com descricao de marketing ou trafego pago");
        }
        if (contemAlguma(descricao,
                "taxa cartao", "taxa de cartao", "taxa maquininha",
                "mdr", "taxa marketplace", "taxa link de pagamento",
                "taxa pix", "taxa venda", "taxas de pedido")) {
            return resultado(
                    ClasseTransacao.TAXAS_VENDAS,
                    "debito com descricao de taxa de venda");
        }
        if (contemAlguma(descricao,
                "comissao vendedor", "comissao venda", "comissao parceiro",
                "comissao indicacao", "bonificacao por venda")) {
            return resultado(
                    ClasseTransacao.COMISSOES_VENDA,
                    "debito com descricao de comissao de venda");
        }
        if (contemAlguma(descricao,
                "whatsapp business", "whatsapp api", "crm", "chatbot",
                "automacao atendimento", "recuperacao de lead", "sistema de catalogo",
                "pedidos online")) {
            return resultado(
                    ClasseTransacao.FERRAMENTAS_VENDA_ATENDIMENTO,
                    "debito com descricao de ferramenta de venda ou atendimento");
        }
        if (contemAlguma(descricao,
                "alimentacao", "refeicao", "almoco", "jantar", "lanche",
                "restaurante", "ifood", "padaria", "mercado alimentacao",
                "vale alimentacao", "vale refeicao")) {
            return resultado(
                    ClasseTransacao.ALIMENTACAO_EQUIPE,
                    "debito com descricao de alimentacao");
        }
        if (contemAlguma(descricao,
                "folha de pagamento", "salario", "salarios", "diaria fixa",
                "encargo trabalhista", "vale transporte",
                "ferias", "13 salario", "decimo terceiro", "rescisao", "fgts")) {
            return resultado(
                    ClasseTransacao.SALARIOS_ENCARGOS,
                    "debito com descricao de salarios ou encargos");
        }
        if (contemAlguma(descricao,
                "aluguel", "condominio", "iptu", "reforma pequena",
                "moveis loja", "manutencao predial", "equipamento loja")) {
            return resultado(
                    ClasseTransacao.ALUGUEL_INFRA,
                    "debito com descricao de aluguel ou infraestrutura");
        }
        if (contemAlguma(descricao,
                "energia eletrica", "conta de energia", "agua", "internet",
                "telefone", "plano de celular")) {
            return resultado(
                    ClasseTransacao.AGUA_ENERGIA_INTERNET_TELEFONE,
                    "debito com descricao de consumo ou comunicacao");
        }
        if (contemAlguma(descricao,
                "sistema", "software", "assinatura", "bling", "nuvemshop",
                "shopify", "dominio", "hospedagem", "google workspace",
                "canva", "saas")) {
            return resultado(
                    ClasseTransacao.SISTEMAS_ASSINATURAS,
                    "debito com descricao de sistema ou assinatura");
        }
        if (contemAlguma(descricao,
                "produto de limpeza", "material de escritorio", "manutencao equipamento",
                "conserto", "ferramenta uso interno", "organizacao loja")) {
            return resultado(
                    ClasseTransacao.MANUTENCAO_LIMPEZA_MATERIAIS,
                    "debito com descricao de manutencao, limpeza ou material interno");
        }
        if (contemAlguma(descricao,
                "contabilidade", "contador", "honorarios contabeis",
                "servicos contabeis", "assessoria contabil", "advogado",
                "consultoria", "certificado digital")) {
            return resultado(
                    ClasseTransacao.CONTABILIDADE_SERVICOS,
                    "debito com descricao de contabilidade ou servicos profissionais");
        }
        if (contemAlguma(descricao,
                "imposto", "tributo", "das", "simples nacional",
                "irpj", "csll", "pis", "cofins", "iss", "icms",
                "taxa municipal", "licenca", "alvara")) {
            return resultado(
                    ClasseTransacao.IMPOSTOS_TRIBUTOS,
                    "debito com descricao de imposto ou tributo");
        }
        if (contemAlguma(descricao,
                "tarifa bancaria", "tarifa de conta", "tarifa pix",
                "tarifa de pix", "tarifa boleto", "pacote de servicos bancarios",
                "manutencao de conta")) {
            return resultado(
                    ClasseTransacao.TARIFAS_BANCARIAS,
                    "debito com descricao de tarifa bancaria");
        }
        if (contemAlguma(descricao,
                "juros", "multa", "encargos financeiros", "iof",
                "juros de emprestimo", "juros de cartao")) {
            return resultado(
                    ClasseTransacao.JUROS_MULTAS,
                    "debito com descricao de juros ou multa");
        }
        if (contemAlguma(descricao,
                "pagamento emprestimo", "parcela de emprestimo",
                "quitacao de credito", "amortizacao de divida")) {
            return resultado(
                    ClasseTransacao.PAGAMENTO_EMPRESTIMO,
                    "debito com descricao de pagamento de emprestimo");
        }
        if (contemAlguma(descricao,
                "geladeira", "camara fria", "computador", "impressora",
                "moveis grandes", "equipamento de producao", "reforma relevante")) {
            return resultado(
                    ClasseTransacao.INVESTIMENTOS_EQUIPAMENTOS,
                    "debito com descricao de investimento ou equipamento");
        }
        return indefinido();
    }

    private Resultado resultado(ClasseTransacao classe, String justificativa) {
        return new Resultado(classe, Confianca.de(new BigDecimal("0.920")), justificativa);
    }

    private Resultado indefinido() {
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
        if (raw == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record Resultado(ClasseTransacao classe, Confianca confianca, String justificativa) {}
}
