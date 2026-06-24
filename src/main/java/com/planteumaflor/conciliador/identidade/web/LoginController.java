package com.planteumaflor.conciliador.identidade.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.onboarding.application.ConsultarOnboarding;
import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renderiza a tela de login (tela 01).
 *
 * Só o GET é nosso: o POST de credenciais (/entrar) é processado pelo filtro do
 * Spring Security, não por um método aqui.
 */
@Controller
class LoginController {

    private final ConsultarOnboarding onboarding;

    LoginController(ConsultarOnboarding onboarding) {
        this.onboarding = onboarding;
    }

    @GetMapping("/entrar")
    String entrar(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
            EtapaOnboarding etapa = onboarding.etapaAtual(principal.empresaId());
            return etapa == EtapaOnboarding.CONCLUIDO
                    ? "redirect:/inicio"
                    : "redirect:/onboarding";
        }
        return "auth/login";
    }
}
