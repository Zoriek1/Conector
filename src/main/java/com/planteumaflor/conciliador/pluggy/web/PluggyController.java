package com.planteumaflor.conciliador.pluggy.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.pluggy.application.ConectarPluggy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Ações da integração Pluggy iniciadas no app (tela 03/07).
 *
 * O {@code empresaId} vem SEMPRE do principal autenticado, nunca do navegador.
 */
@Controller
@RequestMapping("/integracoes/pluggy")
class PluggyController {

    private final ConectarPluggy conectarPluggy;

    PluggyController(ConectarPluggy conectarPluggy) {
        this.conectarPluggy = conectarPluggy;
    }

    @PostMapping("/conectar")
    String conectar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        conectarPluggy.conectar(principal.empresaId());
        return "redirect:/onboarding";
    }
}
