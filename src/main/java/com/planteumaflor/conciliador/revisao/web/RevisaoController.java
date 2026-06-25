package com.planteumaflor.conciliador.revisao.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.revisao.query.ConsultarFilaRevisao;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Primeira fatia da Tela 05: fila paginada, autenticada e somente leitura. */
@Controller
class RevisaoController {

    private final ConsultarFilaRevisao consultas;

    RevisaoController(ConsultarFilaRevisao consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/revisao")
    String fila(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        var ordenacao = Sort.by(Sort.Order.asc("data"), Sort.Order.asc("createdAt"));
        var pageable = PageRequest.of(Math.max(page, 0), 20, ordenacao);
        model.addAttribute("itens", consultas.consultar(principal.empresaId(), pageable));
        model.addAttribute("email", principal.getUsername());
        return "revisao/index";
    }
}
