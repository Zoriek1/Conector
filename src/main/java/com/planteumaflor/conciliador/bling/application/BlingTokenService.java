package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.bling.domain.BlingToken;
import com.planteumaflor.conciliador.bling.domain.TipoFalhaBling;
import com.planteumaflor.conciliador.bling.persistence.BlingTokenJpaRepository;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Fornece um access token válido por empresa, escondendo a renovação do resto
 * do sistema (Backend §9.2). O refresh é serializado por lock pessimista na
 * linha do token e, se o Bling revogar o refresh, a integração entra em estado
 * terminal ({@code DESCONECTADA}) para parar as escritas até a reconexão.
 */
@Service
public class BlingTokenService {

    private static final Duration MARGEM_PADRAO = Duration.ofMinutes(2);

    private final BlingTokenJpaRepository tokens;
    private final BlingOAuthGateway gateway;
    private final CriptoService cripto;
    private final Duration margemExpiracao;
    private final Clock clock;

    BlingTokenService(
            BlingTokenJpaRepository tokens,
            BlingOAuthGateway gateway,
            CriptoService cripto,
            ConciliadorProperties properties,
            Clock clock) {
        this.tokens = tokens;
        this.gateway = gateway;
        this.cripto = cripto;
        this.margemExpiracao = properties.bling().margemExpiracao() == null
                ? MARGEM_PADRAO
                : properties.bling().margemExpiracao();
        this.clock = clock;
    }

    /**
     * Access token em claro, renovado se estiver perto de expirar. Falha com
     * {@link FalhaBlingException} se a empresa não está conectada, está
     * desconectada (refresh revogado) ou a renovação falhou.
     */
    @Transactional
    public String accessTokenValido(UUID empresaId) {
        BlingToken token = tokens.findByEmpresaIdForUpdate(empresaId)
                .orElseThrow(() -> new FalhaBlingException(
                        TipoFalhaBling.AUTENTICACAO, "empresa sem conexão Bling"));

        if (token.desconectado()) {
            throw new FalhaBlingException(
                    TipoFalhaBling.AUTENTICACAO, "conexão Bling desconectada; reconecte a empresa");
        }

        if (token.precisaRenovar(margemExpiracao, clock.instant())) {
            renovar(token);
        }
        return cripto.decifrar(token.getAccessTokenCifrado());
    }

    private void renovar(BlingToken token) {
        String refresh = cripto.decifrar(token.getRefreshTokenCifrado());
        try {
            BlingOAuthGateway.TokensBling novos = gateway.renovar(refresh);
            token.atualizarTokens(
                    cripto.cifrar(novos.accessToken()),
                    cripto.cifrar(novos.refreshToken()),
                    novos.expiraEm(),
                    clock.instant());
            tokens.save(token);
        } catch (FalhaBlingException e) {
            if (e.tipo() == TipoFalhaBling.AUTENTICACAO) {
                token.registrarRevogacao(clock.instant()); // refresh revogado: estado terminal
            } else {
                token.registrarFalha(e.tipo(), clock.instant());
            }
            tokens.save(token);
            throw e;
        }
    }
}
