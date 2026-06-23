package com.planteumaflor.conciliador.identidade.security;

import com.planteumaflor.conciliador.onboarding.application.ConsultarOnboarding;
import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide para onde mandar o usuário após o login (tela 01):
 * empresa sem Pluggy ativo → /onboarding; configurada → /inicio.
 *
 * Usa {@code request.getContextPath()} para respeitar o context-path {@code /page}.
 */
@Component
class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ConsultarOnboarding onboarding;

    LoginSuccessHandler(ConsultarOnboarding onboarding) {
        this.onboarding = onboarding;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        EtapaOnboarding etapa = onboarding.etapaAtual(principal.empresaId());

        String destino = (etapa == EtapaOnboarding.CONCLUIDO) ? "/inicio" : "/onboarding";
        response.sendRedirect(request.getContextPath() + destino);
    }
}
