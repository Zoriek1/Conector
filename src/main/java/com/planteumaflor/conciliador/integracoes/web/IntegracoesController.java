package com.planteumaflor.conciliador.integracoes.web;

import com.planteumaflor.conciliador.bling.persistence.BlingTokenJpaRepository;
import com.planteumaflor.conciliador.conta.application.ContaBancariaService;
import com.planteumaflor.conciliador.conta.domain.ContaBancaria;
import com.planteumaflor.conciliador.conta.domain.TipoContaBancaria;
import com.planteumaflor.conciliador.cora.persistence.IntegracaoCoraJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.integracoes.RotulosIntegracao;
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
    private final BlingTokenJpaRepository bling;
    private final ContaBancariaService contas;

    IntegracoesController(
            IntegracaoCoraJpaRepository cora,
            IntegracaoPluggyJpaRepository pluggy,
            BlingTokenJpaRepository bling,
            ContaBancariaService contas) {
        this.cora = cora;
        this.pluggy = pluggy;
        this.bling = bling;
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
                        RotulosIntegracao.status(i.getStatus().name()),
                        true,
                        i.getStatus().name().equals("ATIVA"),
                        i.getConectadoEm(),
                        i.getUltimaSincronizacao(),
                        RotulosIntegracao.falha(i.getUltimaFalhaTipo() == null ? null : i.getUltimaFalhaTipo().name()),
                        i.getFalhasConsecutivas()))
                .orElse(IntegracaoCardView.pendente("Cora")));
        model.addAttribute("pluggy", pluggy.findByEmpresaId(empresaId)
                .map(i -> new IntegracaoCardView(
                        "Pluggy",
                        i.getStatus().name(),
                        RotulosIntegracao.status(i.getStatus().name()),
                        i.getClientIdCifrado() != null,
                        i.getPluggyItemId() != null && !i.getPluggyItemId().isBlank(),
                        i.getConectadoEm(),
                        i.getUltimaSincronizacao(),
                        RotulosIntegracao.falha(i.getUltimaFalhaTipo()),
                        i.getFalhasConsecutivas()))
                .orElse(IntegracaoCardView.pendente("Pluggy")));
        model.addAttribute("bling", bling.findByEmpresaId(empresaId)
                .map(t -> new IntegracaoCardView(
                        "Bling",
                        t.getStatus().name(),
                        RotulosIntegracao.status(t.getStatus().name()),
                        true,
                        t.getStatus().name().equals("ATIVA"),
                        t.getConectadoEm(),
                        t.getUltimaRenovacao(),
                        RotulosIntegracao.falha(t.getUltimaFalhaTipo() == null ? null : t.getUltimaFalhaTipo().name()),
                        t.getFalhasConsecutivas()))
                .orElse(IntegracaoCardView.pendente("Bling")));
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
            String statusRotulo,
            boolean credenciaisSalvas,
            boolean itemConectado,
            Instant conectadoEm,
            Instant ultimaSincronizacao,
            String ultimaFalha,
            int falhasConsecutivas) {
        static IntegracaoCardView pendente(String nome) {
            return new IntegracaoCardView(
                    nome, "NAO_CONECTADA", "Não conectada", false, false, null, null, null, 0);
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
                    conta.getFonte().getRotulo(),
                    conta.getIdContaExterna(),
                    conta.getNome(),
                    conta.getBancoCodigo(),
                    conta.getAgencia(),
                    conta.getNumero(),
                    conta.getDigito(),
                    conta.getTipo().getRotulo(),
                    conta.isAtiva(),
                    conta.getUltimaSincronizacao());
        }
    }
}
