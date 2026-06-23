package com.planteumaflor.conciliador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Fornece um {@link Clock} injetável (Backend §7.3).
 *
 * Domínio e workers nunca chamam {@code Instant.now()} direto: recebem o Clock.
 * Assim expiração de token, janelas de ingestão e backoff do outbox ficam
 * testáveis com um relógio fixo.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
