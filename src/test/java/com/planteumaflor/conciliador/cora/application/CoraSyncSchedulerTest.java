package com.planteumaflor.conciliador.cora.application;

import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoraSyncSchedulerTest {

    @Test
    void tentaNovamenteAteSincronizar() {
        IntegracaoCoraJpaRepository integracoes = mock(IntegracaoCoraJpaRepository.class);
        SincronizarExtratoCora sincronizar = mock(SincronizarExtratoCora.class);
        ConciliadorProperties properties = mock(ConciliadorProperties.class);
        ConciliadorProperties.Ingest ingest = mock(ConciliadorProperties.Ingest.class);
        UUID empresaId = UUID.randomUUID();

        when(properties.ingest()).thenReturn(ingest);
        when(ingest.maxTentativas()).thenReturn(3);
        when(ingest.atrasoRetry()).thenReturn(Duration.ZERO);
        when(integracoes.findEmpresaIdsByStatusIn(anyCollection())).thenReturn(List.of(empresaId));
        doThrow(new IllegalStateException("transitória"))
                .doThrow(new IllegalStateException("transitória"))
                .doNothing()
                .when(sincronizar).sincronizar(empresaId);

        new CoraSyncScheduler(integracoes, sincronizar, properties).sincronizarIntegracoes();

        verify(sincronizar, org.mockito.Mockito.times(3)).sincronizar(empresaId);
    }
}
