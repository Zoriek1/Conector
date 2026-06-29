package com.planteumaflor.conciliador.pluggy.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.pluggy.application.PluggyGateway;
import com.planteumaflor.conciliador.pluggy.application.PluggyIntegrationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ações da integração Pluggy iniciadas no app (tela 03/07).
 *
 * O {@code empresaId} vem SEMPRE do principal autenticado, nunca do navegador.
 */
@Controller
@RequestMapping("/integracoes/pluggy")
class PluggyController {

    private final PluggyIntegrationService pluggy;

    PluggyController(PluggyIntegrationService pluggy) {
        this.pluggy = pluggy;
    }

    @PostMapping("/credenciais")
    String credenciais(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam String clientId,
            @RequestParam String clientSecret,
            RedirectAttributes redirect) {
        try {
            pluggy.salvarCredenciais(principal.empresaId(), clientId, clientSecret);
            redirect.addFlashAttribute("sucesso", "Credenciais Pluggy salvas com sucesso.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", mensagemErroPluggy(e, "Não foi possível validar o Pluggy."));
        }
        return "redirect:/integracoes";
    }

    @PostMapping("/conectar")
    String conectar(@AuthenticationPrincipal UsuarioPrincipal principal, RedirectAttributes redirect) {
        try {
            PluggyGateway.ConnectToken token = pluggy.criarConnectToken(principal.empresaId());
            redirect.addFlashAttribute("pluggyConnectToken", token.valor());
            redirect.addFlashAttribute("sucesso", "Connect token Pluggy gerado. Use-o no widget.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", mensagemErroPluggy(e, "Não foi possível iniciar o Pluggy Connect."));
        }
        return "redirect:/integracoes";
    }

    @PostMapping("/retorno")
    String retorno(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam String itemId,
            RedirectAttributes redirect) {
        try {
            pluggy.confirmarItem(principal.empresaId(), itemId);
            redirect.addFlashAttribute("sucesso", "Item Pluggy conectado e contas importadas.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", mensagemErroPluggy(e, "Não foi possível confirmar o item Pluggy."));
        }
        return "redirect:/integracoes";
    }

    @PostMapping("/sincronizar")
    String sincronizar(@AuthenticationPrincipal UsuarioPrincipal principal, RedirectAttributes redirect) {
        try {
            int inseridas = pluggy.sincronizar(principal.empresaId());
            redirect.addFlashAttribute("sucesso", "Pluggy sincronizado. Novas transações: " + inseridas + ".");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", mensagemErroPluggy(e, "Não foi possível sincronizar o Pluggy."));
        }
        return "redirect:/integracoes";
    }

    private String mensagemErroPluggy(RuntimeException e, String padrao) {
        if (e.getMessage() != null && e.getMessage().contains("CRIPTO_KEY")) {
            return "Configure CRIPTO_KEY antes de salvar credenciais.";
        }
        return padrao;
    }
}
