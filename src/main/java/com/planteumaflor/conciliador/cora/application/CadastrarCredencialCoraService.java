package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.config.CriptoService;
import com.planteumaflor.conciliador.cora.domain.IntegracaoCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Valida a credencial chamando {@link CoraGateway#saldo} (falha rápido se
 * clientId/certificado/chave estiverem errados) e só então cifra e persiste.
 */
@Service
class CadastrarCredencialCoraService implements CadastrarCredencialCora {

    private final CoraGateway gateway;
    private final IntegracaoCoraJpaRepository integracoes;
    private final CriptoService cripto;
    private final Clock clock;

    CadastrarCredencialCoraService(
            CoraGateway gateway,
            IntegracaoCoraJpaRepository integracoes,
            CriptoService cripto,
            Clock clock) {
        this.gateway = gateway;
        this.integracoes = integracoes;
        this.cripto = cripto;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cadastrar(UUID empresaId, String clientId, String certificadoPem, String chavePrivadaPem) {
        gateway.saldo(new CoraGateway.CredenciaisCora(clientId, certificadoPem, chavePrivadaPem));

        String clientIdCifrado = cripto.cifrar(clientId);
        String certificadoCifrado = cripto.cifrar(certificadoPem);
        String chavePrivadaCifrada = cripto.cifrar(chavePrivadaPem);
        Instant agora = clock.instant();

        IntegracaoCora integracao = integracoes.findByEmpresaId(empresaId)
                .map(existente -> {
                    existente.atualizarCredenciais(
                            clientIdCifrado, certificadoCifrado, chavePrivadaCifrada, agora);
                    return existente;
                })
                .orElseGet(() -> IntegracaoCora.conectada(
                        empresaId, clientIdCifrado, certificadoCifrado, chavePrivadaCifrada, agora));

        integracoes.save(integracao);
    }
}
