package com.planteumaflor.conciliador.transacao.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.transacao.application.ConsultarTransacoes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela mínima de Transações (lista paginada, sem filtros avançados). */
@Controller
class TransacaoController {

    private final ConsultarTransacoes consultarTransacoes;

    TransacaoController(ConsultarTransacoes consultarTransacoes) {
        this.consultarTransacoes = consultarTransacoes;
    }

    @GetMapping("/transacoes")
    String listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        model.addAttribute("transacoes", consultarTransacoes.listar(principal.empresaId(), pageable));
        model.addAttribute("email", principal.getUsername());
        return "transacoes/index";
    }
}
