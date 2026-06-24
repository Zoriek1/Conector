package com.planteumaflor.conciliador.onboarding.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.onboarding.application.ConsultarOnboarding;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de onboarding (tela 03). A etapa é derivada no servidor; a view só
 * decide o que mostrar a partir dela.
 */
@Controller
class OnboardingController {

    private final ConsultarOnboarding onboarding;

    OnboardingController(ConsultarOnboarding onboarding) {
        this.onboarding = onboarding;
    }

    @GetMapping("/onboarding")
    String onboarding(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        model.addAttribute("etapa", onboarding.etapaAtual(principal.empresaId()));
        model.addAttribute("email", principal.getUsername());
        return "onboarding/index";
    }
}
