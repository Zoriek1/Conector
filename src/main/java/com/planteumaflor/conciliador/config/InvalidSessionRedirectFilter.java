package com.planteumaflor.conciliador.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Redireciona somente requisições protegidas que chegam com um ID de sessão
 * expirado. Evita usar invalidSessionUrl, que no Spring Security 7 também trata
 * violações CSRF como sessão inválida.
 */
final class InvalidSessionRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
            // Gera um novo cookie de sessão para evitar loop com o ID expirado.
            request.getSession();
            response.sendRedirect(request.getContextPath() + "/entrar?expirada");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getServletPath();
        return caminho.equals("/entrar")
                || caminho.equals("/cadastro")
                || caminho.startsWith("/css/")
                || caminho.startsWith("/js/")
                || caminho.startsWith("/img/")
                || caminho.startsWith("/webjars/")
                || caminho.startsWith("/actuator/");
    }
}
