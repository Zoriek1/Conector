package com.planteumaflor.conciliador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Segurança base (Backend §11).
 *
 * Form login + sessão + CSRF (habilitado por padrão). Tudo exige autenticação,
 * exceto login/cadastro, recursos estáticos e o health do Actuator.
 *
 * A tela de login é a nossa ({@code /entrar}, tela 01). O POST de credenciais é
 * processado pelo próprio Spring Security (loginProcessingUrl). O carregamento
 * do usuário vem do nosso UsuarioDetailsService, descoberto automaticamente.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    AuthenticationSuccessHandler successHandler,
                                    RequestCache requestCache,
                                    SecurityContextRepository securityContextRepository,
                                    SessionRegistry sessionRegistry) throws Exception {
        http
                .addFilterBefore(new InvalidSessionRedirectFilter(), SecurityContextHolderFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/entrar", "/cadastro").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                // Registra as sessões (sem limite) para permitir encerrar as
                // demais a partir do perfil (tela 09 §9).
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry))
                .formLogin(form -> form
                        .loginPage("/entrar")            // GET: nossa tela Thymeleaf
                        .loginProcessingUrl("/entrar")   // POST: processado pelo Spring
                        .successHandler(successHandler)  // decide onboarding vs inicio
                        .failureUrl("/entrar?erro")
                        .permitAll()
                )
                .requestCache(cache -> cache.requestCache(requestCache))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .logout(logout -> logout
                        .logoutUrl("/sair")
                        .logoutSuccessUrl("/entrar?saiu")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        // O login deve voltar exatamente à URL original, sem adicionar ?continue.
        requestCache.setMatchingRequestParameterName(null);
        return requestCache;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Mantém o mapa de sessões por principal, base do "encerrar outras sessões". */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Propaga eventos de criação/expiração de sessão para o {@link SessionRegistry}. */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
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
