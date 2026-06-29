package com.planteumaflor.conciliador.transacao.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.transacao.application.ConsultarTransacoes;
import com.planteumaflor.conciliador.transacao.application.TransferenciasInternas;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/** Tela mínima de Transações (lista paginada, sem filtros avançados). */
@Controller
class TransacaoController {

    private final ConsultarTransacoes consultarTransacoes;
    private final TransacaoRepository transacoes;
    private final TransferenciasInternas transferenciasInternas;

    TransacaoController(
            ConsultarTransacoes consultarTransacoes,
            TransacaoRepository transacoes,
            TransferenciasInternas transferenciasInternas) {
        this.consultarTransacoes = consultarTransacoes;
        this.transacoes = transacoes;
        this.transferenciasInternas = transferenciasInternas;
    }

    @GetMapping("/transacoes")
    String listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) EstadoTransacao estado,
            @RequestParam(defaultValue = "false") boolean incluirTransferencias,
            Model model) {
        model.addAttribute("transacoes", estado == null
                ? consultarTransacoes.listar(principal.empresaId(), incluirTransferencias, pageable)
                : transacoes.listarPorEstado(principal.empresaId(), estado, pageable));
        model.addAttribute("estado", estado);
        model.addAttribute("incluirTransferencias", incluirTransferencias);
        model.addAttribute("email", principal.getUsername());
        return "transacoes/index";
    }

    @PostMapping("/transacoes/{id}/transferencia/desfazer")
    String desfazerTransferencia(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id) {
        transferenciasInternas.desfazer(principal.empresaId(), id);
        return "redirect:/transacoes?incluirTransferencias=true";
    }
}
