package com.planteumaflor.conciliador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da aplicação.
 *
 * {@code @ConfigurationPropertiesScan} registra os {@code @ConfigurationProperties}
 * (ex.: {@link com.planteumaflor.conciliador.config.ConciliadorProperties}) sem
 * precisar listá-los um a um.
 *
 * {@code @EnableAsync} habilita métodos {@code @Async} (ex.: processamento de
 * webhooks Pluggy); com {@code spring.threads.virtual.enabled=true}, o Spring
 * Boot já usa virtual threads como executor padrão, sem bean extra.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class ConciliadorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConciliadorApplication.class, args);
	}

}
