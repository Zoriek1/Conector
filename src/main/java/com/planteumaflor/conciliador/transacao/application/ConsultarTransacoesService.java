package com.planteumaflor.conciliador.transacao.application;

import com.planteumaflor.conciliador.transacao.domain.Transacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class ConsultarTransacoesService implements ConsultarTransacoes {

    private final TransacaoRepository transacoes;

    ConsultarTransacoesService(TransacaoRepository transacoes) {
        this.transacoes = transacoes;
    }

    @Override
    public Page<Transacao> listar(UUID empresaId, boolean incluirTransferencias, Pageable pageable) {
        return incluirTransferencias
                ? transacoes.listarPorEmpresa(empresaId, pageable)
                : transacoes.listarNaoPareadas(empresaId, pageable);
    }
}
