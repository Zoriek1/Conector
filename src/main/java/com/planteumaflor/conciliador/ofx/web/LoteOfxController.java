package com.planteumaflor.conciliador.ofx.web;

import com.planteumaflor.conciliador.conta.persistence.ContaBancariaJpaRepository;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import com.planteumaflor.conciliador.ofx.application.OfxService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/ofx/lotes")
class LoteOfxController {

    private final OfxService ofx;
    private final ContaBancariaJpaRepository contas;

    LoteOfxController(OfxService ofx, ContaBancariaJpaRepository contas) {
        this.ofx = ofx;
        this.contas = contas;
    }

    @GetMapping
    String listar(@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
        model.addAttribute("email", principal.getUsername());
        model.addAttribute("lotes", ofx.listar(principal.empresaId()));
        model.addAttribute("contas", contas.findByEmpresaIdAndAtivaTrueOrderByFonteAscNomeAsc(principal.empresaId()));
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("inicioPadrao", LocalDate.now().minusDays(30));
        return "ofx/index";
    }

    @PostMapping
    String gerar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam UUID contaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            RedirectAttributes redirect) {
        try {
            UUID loteId = ofx.gerar(principal.empresaId(), contaId, inicio, fim);
            redirect.addFlashAttribute("sucesso", "Lote OFX gerado: " + loteId + ".");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", "Não foi possível gerar o lote OFX.");
        }
        return "redirect:/ofx/lotes";
    }

    @GetMapping("/{id}/arquivo")
    ResponseEntity<byte[]> arquivo(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id) {
        OfxService.ArquivoOfx arquivo = ofx.obterArquivo(principal.empresaId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nomeArquivo())
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(arquivo.mediaType()))
                .body(arquivo.conteudo());
    }

    @PostMapping("/{id}/confirmar")
    String confirmar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) String observacao,
            RedirectAttributes redirect) {
        try {
            ofx.confirmarUpload(principal.empresaId(), id, observacao);
            redirect.addFlashAttribute("sucesso", "Upload OFX confirmado.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", "Não foi possível confirmar o upload.");
        }
        return "redirect:/ofx/lotes";
    }
}
