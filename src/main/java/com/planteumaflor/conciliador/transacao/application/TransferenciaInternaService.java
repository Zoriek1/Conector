package com.planteumaflor.conciliador.transacao.application;

import com.planteumaflor.conciliador.transacao.domain.Direcao;
import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Normaliza transferências bancárias de mesma titularidade: quando uma conta recebe
 * um valor X numa data e outra conta da mesma empresa tem uma saída do mesmo valor X
 * na mesma data, as duas pernas são pareadas e ocultadas das listas por padrão.
 *
 * <p>Para evitar falso positivo, o pareamento automático só acontece quando o
 * casamento é inequívoco: exatamente uma entrada e uma saída para um par
 * (data, valor) e em contas distintas. Grupos ambíguos ficam para revisão manual.
 */
@Service
@Transactional
class TransferenciaInternaService implements TransferenciasInternas {

    private final TransacaoRepository transacoes;

    TransferenciaInternaService(TransacaoRepository transacoes) {
        this.transacoes = transacoes;
    }

    @Override
    public int detectar(UUID empresaId) {
        Map<Chave, Grupo> grupos = new LinkedHashMap<>();
        for (Transacao candidata : transacoes.listarCandidatosTransferencia(empresaId)) {
            grupos.computeIfAbsent(Chave.de(candidata), c -> new Grupo()).adicionar(candidata);
        }

        int pares = 0;
        for (Grupo grupo : grupos.values()) {
            if (grupo.ehParInequivoco()) {
                Transacao credito = grupo.creditos.get(0);
                Transacao debito = grupo.debitos.get(0);
                credito.detectarTransferenciaInterna(debito.getId());
                debito.detectarTransferenciaInterna(credito.getId());
                transacoes.salvar(credito);
                transacoes.salvar(debito);
                pares++;
            }
        }
        return pares;
    }

    @Override
    public void desfazer(UUID empresaId, UUID transacaoId) {
        Transacao transacao = transacoes.buscarPorId(empresaId, transacaoId)
                .orElseThrow(() -> new IllegalArgumentException("transação não encontrada"));
        UUID parId = transacao.getTransferParId();
        transacao.desfazerTransferenciaInterna();
        transacoes.salvar(transacao);
        if (parId != null) {
            transacoes.buscarPorId(empresaId, parId).ifPresent(par -> {
                par.desfazerTransferenciaInterna();
                transacoes.salvar(par);
            });
        }
    }

    /** Chave de agrupamento por data e valor (escala fixa em 2 casas, como persistido). */
    private record Chave(LocalDate data, BigDecimal valor) {
        static Chave de(Transacao transacao) {
            return new Chave(transacao.getData(), transacao.getValorLiquido().stripTrailingZeros());
        }
    }

    private static final class Grupo {
        private final List<Transacao> creditos = new ArrayList<>();
        private final List<Transacao> debitos = new ArrayList<>();

        void adicionar(Transacao transacao) {
            if (transacao.getDirecao() == Direcao.CREDITO) {
                creditos.add(transacao);
            } else {
                debitos.add(transacao);
            }
        }

        boolean ehParInequivoco() {
            return creditos.size() == 1
                    && debitos.size() == 1
                    && contasDistintas(creditos.get(0), debitos.get(0));
        }

        private boolean contasDistintas(Transacao credito, Transacao debito) {
            return credito.getFonte() != debito.getFonte()
                    || !credito.getIdContaExterna().equals(debito.getIdContaExterna());
        }
    }
}
