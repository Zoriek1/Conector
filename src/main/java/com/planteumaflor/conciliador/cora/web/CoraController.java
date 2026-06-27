package com.planteumaflor.conciliador.cora.web;

import com.planteumaflor.conciliador.cora.application.CadastrarCredencialCora;
import com.planteumaflor.conciliador.cora.application.SincronizarExtratoCora;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    String formulario() {
        return "redirect:/integracoes";
    }

    @PostMapping({"/conectar", "/credenciais"})
    String conectar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam String clientId,
            @RequestParam String certificadoPem,
            @RequestParam String chavePrivadaPem,
            RedirectAttributes redirect) {
        try {
            cadastrarCredencial.cadastrar(principal.empresaId(), clientId, certificadoPem, chavePrivadaPem);
            redirect.addFlashAttribute("sucesso", "Credencial do Cora cadastrada com sucesso.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", mensagemErroCora(e, "Não foi possível validar a credencial do Cora."));
        }
        return "redirect:/integracoes";
    }

    @PostMapping("/sincronizar")
    String sincronizar(@AuthenticationPrincipal UsuarioPrincipal principal, RedirectAttributes redirect) {
        try {
            sincronizarExtrato.sincronizar(principal.empresaId());
            redirect.addFlashAttribute("sucesso", "Extrato do Cora sincronizado com sucesso.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", mensagemErroCora(
                    e, "Não foi possível sincronizar o extrato do Cora. Tente novamente."));
        }
        return "redirect:/integracoes";
    }

    private String mensagemErroCora(RuntimeException e, String padrao) {
        if (e.getMessage() != null && e.getMessage().contains("CRIPTO_KEY")) {
            return "Configure CRIPTO_KEY antes de salvar credenciais.";
        }
        if (e.getMessage() != null && e.getMessage().contains("mTLS")) {
            return "Certificado ou chave privada da Cora inválidos.";
        }
        return padrao;
    }
}
