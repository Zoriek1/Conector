package com.planteumaflor.conciliador.conta.application;

import com.planteumaflor.conciliador.conta.domain.ContaBancaria;
import com.planteumaflor.conciliador.conta.domain.TipoContaBancaria;
import com.planteumaflor.conciliador.conta.persistence.ContaBancariaJpaRepository;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ContaBancariaService {

    private final ContaBancariaJpaRepository contas;
    private final Clock clock;

    public ContaBancariaService(ContaBancariaJpaRepository contas, Clock clock) {
        this.contas = contas;
        this.clock = clock;
    }

    @Transactional
    public ContaBancaria salvarOuAtualizar(
            UUID empresaId,
            FonteIntegracao fonte,
            String idContaExterna,
            String nome,
            String bancoCodigo,
            String agencia,
            String numero,
            String digito,
            TipoContaBancaria tipo) {
        ContaBancaria conta = contas.findByEmpresaIdAndFonteAndIdContaExterna(
                        empresaId, fonte, idContaExterna)
                .map(existente -> {
                    existente.atualizarDados(nome, bancoCodigo, agencia, numero, digito, tipo);
                    return existente;
                })
                .orElseGet(() -> ContaBancaria.nova(
                        empresaId, fonte, idContaExterna, nome,
                        bancoCodigo, agencia, numero, digito, tipo));
        return contas.save(conta);
    }

    @Transactional
    public void registrarSincronizacao(UUID empresaId, FonteIntegracao fonte, String idContaExterna) {
        contas.findByEmpresaIdAndFonteAndIdContaExterna(empresaId, fonte, idContaExterna)
                .ifPresent(conta -> conta.registrarSincronizacao(clock.instant()));
    }

    @Transactional(readOnly = true)
    public boolean estaAtiva(UUID empresaId, FonteIntegracao fonte, String idContaExterna) {
        return contas.findByEmpresaIdAndFonteAndIdContaExterna(empresaId, fonte, idContaExterna)
                .map(ContaBancaria::isAtiva)
                .orElse(false);
    }

    @Transactional
    public void alterarStatus(UUID empresaId, UUID contaId, boolean ativa) {
        ContaBancaria conta = contas.findByIdAndEmpresaId(contaId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("conta não encontrada"));
        if (ativa) {
            conta.ativar();
        } else {
            conta.pausar();
        }
    }

    @Transactional(readOnly = true)
    public List<ContaBancaria> listar(UUID empresaId) {
        return contas.findByEmpresaIdOrderByFonteAscNomeAsc(empresaId);
    }
}
