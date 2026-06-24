package com.planteumaflor.conciliador.inicio.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Placeholder atual: prova que o usuário autenticado e o tenant
 * ({@code empresaId}) chegam ao controller pela sessão — nunca pelo navegador.
 *
 * Cards, integrações e atividade permanecem no marco da tela 04.
 */
@Controller
class InicioController {

    @GetMapping("/inicio")
    String inicio(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        model.addAttribute("email", principal.getUsername());
        model.addAttribute("empresaId", principal.empresaId());
        return "inicio/index";
    }
}
