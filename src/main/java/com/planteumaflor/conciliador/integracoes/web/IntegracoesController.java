package com.planteumaflor.conciliador.integracoes.web;

import com.planteumaflor.conciliador.conta.application.ContaBancariaService;
import com.planteumaflor.conciliador.conta.domain.ContaBancaria;
import com.planteumaflor.conciliador.conta.domain.TipoContaBancaria;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.pluggy.persistence.IntegracaoPluggyJpaRepository;
import com.planteumaflor.conciliador.transacao.domain.FonteIntegracao;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
class IntegracoesController {

    private final IntegracaoCoraJpaRepository cora;
    private final IntegracaoPluggyJpaRepository pluggy;
    private final ContaBancariaService contas;

    IntegracoesController(
            IntegracaoCoraJpaRepository cora,
            IntegracaoPluggyJpaRepository pluggy,
            ContaBancariaService contas) {
        this.cora = cora;
        this.pluggy = pluggy;
        this.contas = contas;
    }

    @GetMapping("/integracoes")
    String integracoes(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        UUID empresaId = principal.empresaId();
        model.addAttribute("email", principal.getUsername());
        model.addAttribute("cora", cora.findByEmpresaId(empresaId)
                .map(i -> new IntegracaoCardView(
                        "Cora",
                        i.getStatus().name(),
                        i.getConectadoEm(),
                        i.getUltimaSincronizacao(),
                        i.getUltimaFalhaTipo() == null ? null : i.getUltimaFalhaTipo().name(),
                        i.getFalhasConsecutivas()))
                .orElse(IntegracaoCardView.pendente("Cora")));
        model.addAttribute("pluggy", pluggy.findByEmpresaId(empresaId)
                .map(i -> new IntegracaoCardView(
                        "Pluggy",
                        i.getStatus().name(),
                        i.getConectadoEm(),
                        i.getUltimaSincronizacao(),
                        i.getUltimaFalhaTipo(),
                        i.getFalhasConsecutivas()))
                .orElse(IntegracaoCardView.pendente("Pluggy")));
        model.addAttribute("contas", contas.listar(empresaId).stream()
                .map(ContaView::de)
                .toList());
        model.addAttribute("tiposConta", TipoContaBancaria.values());
        return "integracoes/index";
    }

    @PostMapping("/integracoes/contas")
    String salvarConta(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam FonteIntegracao fonte,
            @RequestParam String idContaExterna,
            @RequestParam String nome,
            @RequestParam(required = false) String bancoCodigo,
            @RequestParam(required = false) String agencia,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String digito,
            @RequestParam TipoContaBancaria tipo,
            RedirectAttributes redirect) {
        contas.salvarOuAtualizar(
                principal.empresaId(), fonte, idContaExterna, nome,
                bancoCodigo, agencia, numero, digito, tipo);
        redirect.addFlashAttribute("sucesso", "Conta bancária salva.");
        return "redirect:/integracoes";
    }

    @PostMapping("/integracoes/contas/{id}/pausar")
    String pausarConta(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            RedirectAttributes redirect) {
        contas.alterarStatus(principal.empresaId(), id, false);
        redirect.addFlashAttribute("sucesso", "Conta pausada para novas sincronizações.");
        return "redirect:/integracoes";
    }

    @PostMapping("/integracoes/contas/{id}/ativar")
    String ativarConta(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            RedirectAttributes redirect) {
        contas.alterarStatus(principal.empresaId(), id, true);
        redirect.addFlashAttribute("sucesso", "Conta ativa para sincronização.");
        return "redirect:/integracoes";
    }

    record IntegracaoCardView(
            String nome,
            String status,
            Instant conectadoEm,
            Instant ultimaSincronizacao,
            String ultimaFalha,
            int falhasConsecutivas) {
        static IntegracaoCardView pendente(String nome) {
            return new IntegracaoCardView(nome, "NAO_CONECTADA", null, null, null, 0);
        }
    }

    record ContaView(
            UUID id,
            String fonte,
            String idContaExterna,
            String nome,
            String bancoCodigo,
            String agencia,
            String numero,
            String digito,
            String tipo,
            boolean ativa,
            Instant ultimaSincronizacao) {
        static ContaView de(ContaBancaria conta) {
            return new ContaView(
                    conta.getId(),
                    conta.getFonte().name(),
                    conta.getIdContaExterna(),
                    conta.getNome(),
                    conta.getBancoCodigo(),
                    conta.getAgencia(),
                    conta.getNumero(),
                    conta.getDigito(),
                    conta.getTipo().name(),
                    conta.isAtiva(),
                    conta.getUltimaSincronizacao());
        }
    }
}
