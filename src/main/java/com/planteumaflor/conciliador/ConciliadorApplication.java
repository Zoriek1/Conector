package com.planteumaflor.conciliador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da aplicação.
 *
 * {@code @ConfigurationPropertiesScan} registra os {@code @ConfigurationProperties}
 * (ex.: {@link com.planteumaflor.conciliador.config.ConciliadorProperties}) sem
 * precisar listá-los um a um.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ConciliadorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConciliadorApplication.class, args);
	}

}
