package com.planteumaflor.conciliador.revisao.web;

import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.revisao.application.ClassificarLoteCommand;
import com.planteumaflor.conciliador.revisao.application.ClassificarLoteCommand.ItemVersao;
import com.planteumaflor.conciliador.revisao.application.RecursoNaoEncontrado;
import com.planteumaflor.conciliador.revisao.application.RevisarTransacao;
import com.planteumaflor.conciliador.revisao.query.ConsultarFilaRevisao;
import com.planteumaflor.conciliador.revisao.query.FiltroRevisao;
import com.planteumaflor.conciliador.transacao.domain.ClasseTransacao;
import com.planteumaflor.conciliador.transacao.domain.Direcao;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Tela 05: fila autenticada com filtros, busca e comandos via HTMX. */
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
            @RequestParam(required = false) Direcao direcao,
            @RequestParam(required = false) ClasseTransacao classe,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Model model) {
        preencherFila(model, principal.empresaId(), new FiltroRevisao(estado, direcao, classe, q), page, size);
        model.addAttribute("email", principal.getUsername());
        return "revisao/index";
    }

    @GetMapping("/revisao/fila")
    String fila(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "EM_REVISAO") EstadoTransacao estado,
            @RequestParam(required = false) Direcao direcao,
            @RequestParam(required = false) ClasseTransacao classe,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Model model) {
        preencherFila(model, principal.empresaId(), new FiltroRevisao(estado, direcao, classe, q), page, size);
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

    @PostMapping("/revisao/lote/classificar")
    String classificarLote(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(name = "ids", required = false) List<String> ids,
            @RequestParam(name = "versions", required = false) List<Long> versions,
            @RequestParam ClasseTransacao classe,
            @RequestParam(required = false) String justificativa,
            @RequestParam(defaultValue = "EM_REVISAO") EstadoTransacao estado,
            @RequestParam(required = false) Direcao direcao,
            @RequestParam(name = "classeFiltro", required = false) ClasseTransacao classeFiltro,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Model model) {
        List<ItemVersao> itens = itensSelecionados(ids, versions);
        revisar.classificarLote(
                principal.empresaId(),
                new ClassificarLoteCommand(itens, classe, justificativa));
        preencherFila(
                model,
                principal.empresaId(),
                new FiltroRevisao(estado, direcao, classeFiltro, q),
                page,
                size);
        model.addAttribute("mensagemLote", itens.size() + " transacao(oes) classificadas");
        return "revisao/fragments :: fila";
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

    private void preencherFila(Model model, UUID empresaId, FiltroRevisao filtro, int page, int size) {
        Sort ordenacao = Sort.by(Sort.Order.asc("data"), Sort.Order.asc("createdAt"));
        int tamanhoPagina = tamanhoPagina(size);
        Pageable pageable = PageRequest.of(Math.max(page, 0), tamanhoPagina, ordenacao);
        model.addAttribute("itens", consultas.consultar(empresaId, filtro, pageable));
        model.addAttribute("filtro", filtro);
        model.addAttribute("estado", filtro.estado());
        model.addAttribute("direcao", filtro.direcao());
        model.addAttribute("classe", filtro.classe());
        model.addAttribute("q", filtro.q());
        model.addAttribute("classes", ClasseTransacao.classificaveis());
        model.addAttribute("classesLote", classesLote(filtro.direcao()));
        model.addAttribute("direcoes", Direcao.values());
        model.addAttribute("tamanhoPagina", tamanhoPagina);
        model.addAttribute("opcoesTamanhoPagina", List.of(20, 50, 100));
    }

    private int tamanhoPagina(int size) {
        return Arrays.stream(new int[] {20, 50, 100})
                .filter(opcao -> opcao == size)
                .findFirst()
                .orElse(100);
    }

    private List<ClasseTransacao> classesLote(Direcao direcao) {
        return direcao == null
                ? ClasseTransacao.classificaveis()
                : ClasseTransacao.classificaveisPara(direcao);
    }

    private String linha(Model model, UUID empresaId, UUID id) {
        var item = consultas.consultarItem(empresaId, id)
                .orElseThrow(() -> new RecursoNaoEncontrado("transacao nao encontrada"));
        model.addAttribute("item", item);
        model.addAttribute("classes", ClasseTransacao.classificaveis());
        return "revisao/fragments :: linha";
    }

    private List<ItemVersao> itensSelecionados(List<String> ids, List<Long> versions) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ItemVersao> itens = new ArrayList<>();
        boolean idsComVersao = ids.stream().allMatch(valor -> valor.contains(":"));
        if (idsComVersao) {
            for (String valor : ids) {
                String[] partes = valor.split(":", 2);
                itens.add(new ItemVersao(UUID.fromString(partes[0]), Long.parseLong(partes[1])));
            }
            return itens;
        }
        if (versions == null || versions.size() != ids.size()) {
            throw new IllegalArgumentException("ids e versions devem ter o mesmo tamanho");
        }
        for (int i = 0; i < ids.size(); i++) {
            itens.add(new ItemVersao(UUID.fromString(ids.get(i)), versions.get(i)));
        }
        return itens;
    }
}
