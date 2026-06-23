package com.planteumaflor.conciliador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança base (Backend §11).
 *
 * Form login + sessão + CSRF (habilitado por padrão). Tudo exige autenticação,
 * exceto login/cadastro, recursos estáticos e o health do Actuator.
 *
 * No esqueleto usamos a página de login padrão do Spring. No passo 2 ela será
 * trocada por {@code loginPage("/entrar")} com template Thymeleaf (tela 01).
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/entrar", "/cadastro").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.permitAll())   // TODO passo 2: loginPage("/entrar")
                .logout(logout -> logout.permitAll());
        return http.build();
    }

    /**
     * Encoder delegante (prefixo {@code {bcrypt}} etc.), recomendado pelo Spring.
     * Permite evoluir o algoritmo sem reescrever hashes antigos.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
