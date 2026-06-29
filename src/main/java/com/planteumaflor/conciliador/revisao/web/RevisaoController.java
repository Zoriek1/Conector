package com.planteumaflor.conciliador.revisao.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.revisao.application.RecursoNaoEncontrado;
import com.planteumaflor.conciliador.revisao.application.RevisarTransacao;
import com.planteumaflor.conciliador.revisao.query.ConsultarFilaRevisao;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.EstadoTransacao;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

/** Tela 05: fila autenticada com comandos de revisão via HTMX (Backend §6.6, §10). */
@Controller
class RevisaoController {

    private final ConsultarFilaRevisao consultas;
    private final RevisarTransacao revisar;

    RevisaoController(ConsultarFilaRevisao consultas, RevisarTransacao revisar) {
        this.consultas = consultas;
        this.revisar = revisar;
    }

    @GetMapping("/revisao")
    String pagina(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "EM_REVISAO") EstadoTransacao estado,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        preencherFila(model, principal.empresaId(), estado, page);
        model.addAttribute("email", principal.getUsername());
        return "revisao/index";
    }

    @GetMapping("/revisao/fila")
    String fila(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "EM_REVISAO") EstadoTransacao estado,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        preencherFila(model, principal.empresaId(), estado, page);
        return "revisao/fragments :: fila";
    }

    @GetMapping("/revisao/{id}")
    String item(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            Model model) {
        return linha(model, principal.empresaId(), id);
    }

    @PostMapping("/revisao/{id}/aprovar")
    String aprovar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @RequestParam long version,
            Model model) {
        revisar.aprovar(principal.empresaId(), id, version);
        return linha(model, principal.empresaId(), id);
    }

    @PostMapping("/revisao/{id}/classificar")
    String classificar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @RequestParam long version,
            @RequestParam ClasseTransacao classe,
            Model model) {
        revisar.classificar(principal.empresaId(), id, version, classe);
        return linha(model, principal.empresaId(), id);
    }

    @PostMapping("/revisao/{id}/match")
    String match(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @RequestParam long version,
            @RequestParam String tipo,
            @RequestParam String idExterno,
            @RequestParam(required = false) BigDecimal taxa,
            Model model) {
        revisar.selecionarMatch(principal.empresaId(), id, version, tipo, idExterno, taxa);
        return linha(model, principal.empresaId(), id);
    }

    @PostMapping("/revisao/{id}/ofx")
    String ofx(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @RequestParam long version,
            Model model) {
        revisar.rotearParaOfx(principal.empresaId(), id, version);
        return linha(model, principal.empresaId(), id);
    }

    @PostMapping("/revisao/{id}/retry")
    String retry(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @RequestParam long version,
            Model model) {
        revisar.solicitarRetry(principal.empresaId(), id, version);
        return linha(model, principal.empresaId(), id);
    }

    private void preencherFila(Model model, UUID empresaId, EstadoTransacao estado, int page) {
        Sort ordenacao = Sort.by(Sort.Order.asc("data"), Sort.Order.asc("createdAt"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, ordenacao);
        model.addAttribute("itens", consultas.consultar(empresaId, estado, pageable));
        model.addAttribute("estado", estado);
        model.addAttribute("classes", ClasseTransacao.values());
    }

    private String linha(Model model, UUID empresaId, UUID id) {
        var item = consultas.consultarItem(empresaId, id)
                .orElseThrow(() -> new RecursoNaoEncontrado("transação não encontrada"));
        model.addAttribute("item", item);
        model.addAttribute("classes", ClasseTransacao.values());
        return "revisao/fragments :: linha";
    }
}
