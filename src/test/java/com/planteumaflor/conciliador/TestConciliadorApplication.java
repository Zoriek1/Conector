package com.planteumaflor.conciliador;

import org.springframework.boot.SpringApplication;

public class TestConciliadorApplication {

	public static void main(String[] args) {
		SpringApplication.from(ConciliadorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
