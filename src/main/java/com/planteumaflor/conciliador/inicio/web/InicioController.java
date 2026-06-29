package com.planteumaflor.conciliador.inicio.web;

import com.planteumaflor.conciliador.cora.application.SincronizarExtratoCora;
import com.planteumaflor.conciliador.cora.domain.StatusIntegracaoCora;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.inicio.query.ConsultarInicio;
import com.planteumaflor.conciliador.pluggy.application.PluggyIntegrationService;
import com.planteumaflor.conciliador.pluggy.domain.StatusIntegracao;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inicio")
class InicioController {

    private final ConsultarInicio consultarInicio;
    private final IntegracaoCoraJpaRepository cora;
    private final IntegracaoPluggyJpaRepository pluggy;
    private final SincronizarExtratoCora sincronizarCora;
    private final PluggyIntegrationService sincronizarPluggy;

    InicioController(
            ConsultarInicio consultarInicio,
            IntegracaoCoraJpaRepository cora,
            IntegracaoPluggyJpaRepository pluggy,
            SincronizarExtratoCora sincronizarCora,
            PluggyIntegrationService sincronizarPluggy) {
        this.consultarInicio = consultarInicio;
        this.cora = cora;
        this.pluggy = pluggy;
        this.sincronizarCora = sincronizarCora;
        this.sincronizarPluggy = sincronizarPluggy;
    }

    @GetMapping
    String inicio(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        preencher(principal, model);
        return "inicio/index";
    }

    @GetMapping("/resumo")
    String resumo(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        preencher(principal, model);
        return "inicio/index :: resumo";
    }

    @PostMapping("/sincronizar")
    String sincronizar(@AuthenticationPrincipal UsuarioPrincipal principal, RedirectAttributes redirect) {
        int conectores = 0;
        int novasPluggy = 0;
        try {
            if (cora.findByEmpresaId(principal.empresaId())
                    .map(i -> i.getStatus() == StatusIntegracaoCora.ATIVA
                            || i.getStatus() == StatusIntegracaoCora.REQUER_ATENCAO)
                    .orElse(false)) {
                sincronizarCora.sincronizar(principal.empresaId());
                conectores++;
            }
            if (pluggy.findByEmpresaId(principal.empresaId())
                    .map(i -> i.getStatus() == StatusIntegracao.ATIVA
                            || i.getStatus() == StatusIntegracao.REQUER_ATENCAO)
                    .orElse(false)) {
                novasPluggy = sincronizarPluggy.sincronizar(principal.empresaId());
                conectores++;
            }
            redirect.addFlashAttribute("sucesso",
                    conectores == 0
                            ? "Nenhum conector ativo para sincronizar."
                            : "Sincronização concluída. Novas transações Pluggy: " + novasPluggy + ".");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", "Não foi possível concluir a sincronização.");
        }
        return "redirect:/inicio";
    }

    private void preencher(UsuarioPrincipal principal, Model model) {
        model.addAttribute("email", principal.getUsername());
        model.addAttribute("inicio", consultarInicio.consultar(principal.empresaId()));
    }
}
