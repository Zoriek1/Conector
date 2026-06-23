package com.planteumaflor.conciliador.inicio.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Placeholder do passo 2: prova que o usuário autenticado e o tenant
 * ({@code empresaId}) chegam ao controller pela sessão — nunca pelo navegador.
 *
 * No passo 3 esta tela ganha cards, integrações e atividade (tela 04).
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
