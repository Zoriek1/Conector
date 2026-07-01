package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.bling.domain.BlingToken;
import com.planteumaflor.conciliador.bling.persistence.BlingTokenJpaRepository;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Orquestra a conexão OAuth com o Bling: monta a URL de autorização e, no
 * retorno, troca o {@code code} por tokens, cifra-os e persiste um
 * {@link BlingToken} por empresa (cria ou atualiza).
 */
@Service
class ConectarBlingService implements ConectarBling {

    private final BlingOAuthGateway gateway;
    private final BlingTokenJpaRepository tokens;
    private final CriptoService cripto;
    private final ConciliadorProperties.Bling config;
    private final Clock clock;

    ConectarBlingService(
            BlingOAuthGateway gateway,
            BlingTokenJpaRepository tokens,
            CriptoService cripto,
            ConciliadorProperties properties,
            Clock clock) {
        this.gateway = gateway;
        this.tokens = tokens;
        this.cripto = cripto;
        this.config = properties.bling();
        this.clock = clock;
    }

    @Override
    public String urlAutorizacao(String state) {
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(config.authorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.clientId())
                .queryParam("state", state);
        if (config.redirectUri() != null && !config.redirectUri().isBlank()) {
            url.queryParam("redirect_uri", config.redirectUri());
        }
        return url.build().encode().toUriString();
    }

    @Override
    @Transactional
    public void concluir(UUID empresaId, String code) {
        if (code == null || code.isBlank()) {
            throw new FalhaBlingException(
                    com.planteumaflor.conciliador.bling.domain.TipoFalhaBling.AUTENTICACAO,
                    "callback do Bling sem code");
        }
        BlingOAuthGateway.TokensBling novos = gateway.trocarCodigo(code, config.redirectUri());

        String accessCifrado = cripto.cifrar(novos.accessToken());
        String refreshCifrado = cripto.cifrar(novos.refreshToken());
        Instant agora = clock.instant();

        BlingToken token = tokens.findByEmpresaId(empresaId)
                .map(existente -> {
                    existente.atualizarTokens(accessCifrado, refreshCifrado, novos.expiraEm(), agora);
                    return existente;
                })
                .orElseGet(() -> BlingToken.conectado(
                        empresaId, accessCifrado, refreshCifrado, novos.expiraEm(), agora));
        tokens.save(token);
    }
}
