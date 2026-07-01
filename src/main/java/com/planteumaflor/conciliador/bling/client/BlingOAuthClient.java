package com.planteumaflor.conciliador.bling.client;

import com.planteumaflor.conciliador.bling.application.BlingOAuthGateway;
import com.planteumaflor.conciliador.bling.application.FalhaBlingException;
import com.planteumaflor.conciliador.bling.domain.TipoFalhaBling;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * Client do endpoint OAuth 2.0 do Bling (Bling-API-v3 §6). Autentica o próprio
 * app por HTTP Basic (client_id:client_secret) e troca/renova tokens via
 * {@code application/x-www-form-urlencoded}.
 *
 * Erros HTTP viram {@link FalhaBlingException} já classificada; o corpo da
 * resposta nunca é logado para não vazar code/refresh token.
 */
@Component
class BlingOAuthClient implements BlingOAuthGateway {

    private final RestClient restClient;
    private final String autorizacaoBasic;
    private final Clock clock;

    BlingOAuthClient(ConciliadorProperties properties, Clock clock) {
        ConciliadorProperties.Bling bling = properties.bling();
        this.restClient = RestClient.builder()
                .baseUrl(bling.tokenUrl())
                .build();
        this.autorizacaoBasic = "Basic " + Base64.getEncoder().encodeToString(
                ((bling.clientId() == null ? "" : bling.clientId()) + ":"
                        + (bling.clientSecret() == null ? "" : bling.clientSecret()))
                        .getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public TokensBling trocarCodigo(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }
        return executar(form);
    }

    @Override
    public TokensBling renovar(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return executar(form);
    }

    private TokensBling executar(MultiValueMap<String, String> form) {
        BlingDtos.TokenResponse resposta;
        try {
            resposta = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, autorizacaoBasic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status.value() == 400 || status.value() == 401, (req, res) -> {
                        throw new FalhaBlingException(TipoFalhaBling.AUTENTICACAO,
                                "Bling recusou as credenciais OAuth (HTTP " + res.getStatusCode().value() + ")");
                    })
                    .onStatus(status -> status.is5xxServerError(), (req, res) -> {
                        throw new FalhaBlingException(TipoFalhaBling.COMUNICACAO,
                                "Bling indisponível (HTTP " + res.getStatusCode().value() + ")");
                    })
                    .body(BlingDtos.TokenResponse.class);
        } catch (FalhaBlingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FalhaBlingException(TipoFalhaBling.COMUNICACAO,
                    "falha de comunicação com o OAuth do Bling", e);
        }

        if (resposta == null || resposta.accessToken() == null || resposta.refreshToken() == null) {
            throw new FalhaBlingException(TipoFalhaBling.DADOS_INVALIDOS,
                    "Bling não retornou access_token/refresh_token");
        }
        long expiresIn = resposta.expiresIn() == null ? 0L : resposta.expiresIn();
        return new TokensBling(
                resposta.accessToken(),
                resposta.refreshToken(),
                clock.instant().plus(Duration.ofSeconds(expiresIn)));
    }
}
