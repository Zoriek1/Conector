package com.planteumaflor.conciliador.transacao.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.transacao.application.ConsultarTransacoes;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import com.planteumaflor.conciliador.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Tela mínima de Transações (lista paginada, sem filtros avançados). */
@Controller
class TransacaoController {

    private final ConsultarTransacoes consultarTransacoes;
    private final TransacaoRepository transacoes;

    TransacaoController(ConsultarTransacoes consultarTransacoes, TransacaoRepository transacoes) {
        this.consultarTransacoes = consultarTransacoes;
        this.transacoes = transacoes;
    }

    @GetMapping("/transacoes")
    String listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) EstadoTransacao estado,
            Model model) {
        model.addAttribute("transacoes", estado == null
                ? consultarTransacoes.listar(principal.empresaId(), pageable)
                : transacoes.listarPorEstado(principal.empresaId(), estado, pageable));
        model.addAttribute("estado", estado);
        model.addAttribute("email", principal.getUsername());
        return "transacoes/index";
    }
}
