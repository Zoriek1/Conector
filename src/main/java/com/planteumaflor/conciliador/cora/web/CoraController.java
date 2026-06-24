package com.planteumaflor.conciliador.cora.web;

import com.planteumaflor.conciliador.cora.application.CadastrarCredencialCora;
import com.planteumaflor.conciliador.cora.application.SincronizarExtratoCora;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Ações da integração direta com o Cora.
 *
 * O {@code empresaId} vem SEMPRE do principal autenticado, nunca do navegador.
 */
@Controller
@RequestMapping("/integracoes/cora")
class CoraController {

    private final CadastrarCredencialCora cadastrarCredencial;
    private final SincronizarExtratoCora sincronizarExtrato;

    CoraController(CadastrarCredencialCora cadastrarCredencial, SincronizarExtratoCora sincronizarExtrato) {
        this.cadastrarCredencial = cadastrarCredencial;
        this.sincronizarExtrato = sincronizarExtrato;
    }

    @GetMapping("/conectar")
    String formulario(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        model.addAttribute("email", principal.getUsername());
        return "cora/conectar";
    }

    @PostMapping("/conectar")
    String conectar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam String clientId,
            @RequestParam String certificadoPem,
            @RequestParam String chavePrivadaPem,
            Model model) {
        model.addAttribute("email", principal.getUsername());
        try {
            cadastrarCredencial.cadastrar(principal.empresaId(), clientId, certificadoPem, chavePrivadaPem);
            model.addAttribute("sucesso", "Credencial do Cora cadastrada com sucesso.");
        } catch (RuntimeException e) {
            model.addAttribute("erro", "Não foi possível validar a credencial do Cora: " + e.getMessage());
        }
        return "cora/conectar";
    }

    @PostMapping("/sincronizar")
    String sincronizar(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        model.addAttribute("email", principal.getUsername());
        try {
            sincronizarExtrato.sincronizar(principal.empresaId());
            model.addAttribute("sucesso", "Extrato do Cora sincronizado com sucesso.");
        } catch (RuntimeException e) {
            model.addAttribute("erro", "Não foi possível sincronizar o extrato do Cora: " + e.getMessage());
        }
        return "cora/conectar";
    }
}
