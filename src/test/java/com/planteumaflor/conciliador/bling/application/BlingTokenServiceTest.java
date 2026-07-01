package com.planteumaflor.conciliador.bling.application;

import com.planteumaflor.conciliador.bling.domain.BlingToken;
import com.planteumaflor.conciliador.bling.domain.StatusBling;
import com.planteumaflor.conciliador.bling.domain.TipoFalhaBling;
import com.planteumaflor.conciliador.bling.persistence.BlingTokenJpaRepository;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.config.CriptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlingTokenServiceTest {

    private static final Instant AGORA = Instant.parse("2026-06-30T12:00:00Z");
    private static final String CHAVE = Base64.getEncoder().encodeToString(new byte[32]);
    private static final UUID EMPRESA = UUID.randomUUID();

    private BlingTokenJpaRepository repo;
    private BlingOAuthGateway gateway;
    private CriptoService cripto;
    private BlingTokenService service;

    @BeforeEach
    void setUp() {
        repo = mock(BlingTokenJpaRepository.class);
        gateway = mock(BlingOAuthGateway.class);
        ConciliadorProperties properties = mock(ConciliadorProperties.class);
        when(properties.cripto()).thenReturn(new ConciliadorProperties.Cripto(CHAVE));
        when(properties.bling()).thenReturn(new ConciliadorProperties.Bling(
                null, null, null, null, null, null, Duration.ofMinutes(2)));
        cripto = new CriptoService(properties);
        service = new BlingTokenService(repo, gateway, cripto, properties, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void retornaTokenSemRenovarQuandoLongeDeExpirar() {
        BlingToken token = tokenComExpiracao(AGORA.plus(Duration.ofHours(1)));
        when(repo.findByEmpresaIdForUpdate(EMPRESA)).thenReturn(Optional.of(token));

        assertThat(service.accessTokenValido(EMPRESA)).isEqualTo("access-atual");
        verify(gateway, never()).renovar(any());
    }

    @Test
    void renovaQuandoPertoDeExpirarEPersisteNovoToken() {
        BlingToken token = tokenComExpiracao(AGORA.plus(Duration.ofSeconds(30)));
        when(repo.findByEmpresaIdForUpdate(EMPRESA)).thenReturn(Optional.of(token));
        when(gateway.renovar("refresh-atual")).thenReturn(new BlingOAuthGateway.TokensBling(
                "access-novo", "refresh-novo", AGORA.plus(Duration.ofHours(1))));

        assertThat(service.accessTokenValido(EMPRESA)).isEqualTo("access-novo");
        verify(repo).save(any(BlingToken.class));
    }

    @Test
    void refreshRevogadoDesconectaEPropaga() {
        BlingToken token = tokenComExpiracao(AGORA.plus(Duration.ofSeconds(30)));
        when(repo.findByEmpresaIdForUpdate(EMPRESA)).thenReturn(Optional.of(token));
        when(gateway.renovar("refresh-atual"))
                .thenThrow(new FalhaBlingException(TipoFalhaBling.AUTENTICACAO, "invalid_grant"));

        assertThatThrownBy(() -> service.accessTokenValido(EMPRESA))
                .isInstanceOf(FalhaBlingException.class);
        assertThat(token.getStatus()).isEqualTo(StatusBling.DESCONECTADA);
        verify(repo).save(token);
    }

    @Test
    void semConexaoFalha() {
        when(repo.findByEmpresaIdForUpdate(EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accessTokenValido(EMPRESA))
                .isInstanceOf(FalhaBlingException.class);
    }

    @Test
    void desconectadaNaoChamaGateway() {
        BlingToken token = tokenComExpiracao(AGORA.plus(Duration.ofHours(1)));
        token.registrarRevogacao(AGORA);
        when(repo.findByEmpresaIdForUpdate(EMPRESA)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.accessTokenValido(EMPRESA))
                .isInstanceOf(FalhaBlingException.class);
        verify(gateway, never()).renovar(any());
    }

    private BlingToken tokenComExpiracao(Instant expiraEm) {
        return BlingToken.conectado(
                EMPRESA,
                cripto.cifrar("access-atual"),
                cripto.cifrar("refresh-atual"),
                expiraEm,
                AGORA.minus(Duration.ofHours(2)));
    }
}
