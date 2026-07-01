package com.planteumaflor.conciliador.bling.web;

import com.planteumaflor.conciliador.bling.application.ConectarBling;
import com.planteumaflor.conciliador.bling.application.EstadoOAuthBling;
import com.planteumaflor.conciliador.bling.application.FalhaBlingException;
import com.planteumaflor.conciliador.config.ConciliadorProperties;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Fluxo OAuth 2.0 (authorization code) do Bling (tela 03 §12 / tela 07).
 *
 * O {@code empresaId} vem SEMPRE do principal autenticado. O {@code state} é
 * assinado e de uso único: o nonce é guardado na sessão ao iniciar e conferido
 * no retorno, fechando CSRF e replay do callback.
 */
@Controller
@RequestMapping("/integracoes/bling")
class BlingOAuthController {

    private static final String SESSAO_NONCE = "bling_oauth_nonce";

    private final ConectarBling conectarBling;
    private final EstadoOAuthBling estado;
    private final ConciliadorProperties.Bling config;

    BlingOAuthController(
            ConectarBling conectarBling,
            EstadoOAuthBling estado,
            ConciliadorProperties properties) {
        this.conectarBling = conectarBling;
        this.estado = estado;
        this.config = properties.bling();
    }

    @PostMapping("/conectar")
    String conectar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            HttpSession sessao,
            RedirectAttributes redirect) {
        if (config.clientId() == null || config.clientId().isBlank()) {
            redirect.addFlashAttribute("erro", "Configure BLING_CLIENT_ID e BLING_CLIENT_SECRET antes de conectar o Bling.");
            return "redirect:/integracoes";
        }
        try {
            EstadoOAuthBling.Emitido emitido = estado.gerar(principal.empresaId());
            sessao.setAttribute(SESSAO_NONCE, emitido.nonce());
            return "redirect:" + conectarBling.urlAutorizacao(emitido.valor());
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", "Não foi possível iniciar a conexão com o Bling.");
            return "redirect:/integracoes";
        }
    }

    @GetMapping("/retorno")
    String retorno(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession sessao,
            RedirectAttributes redirect) {
        Object nonceSessao = sessao.getAttribute(SESSAO_NONCE);
        sessao.removeAttribute(SESSAO_NONCE); // uso único: vale uma vez só

        if (error != null) {
            redirect.addFlashAttribute("erro", "Autorização no Bling foi recusada ou cancelada.");
            return "redirect:/integracoes";
        }
        try {
            EstadoOAuthBling.Conteudo conteudo = estado.validar(state);
            UUID empresaId = principal.empresaId();
            if (!empresaId.equals(conteudo.empresaId())
                    || nonceSessao == null
                    || !nonceSessao.equals(conteudo.nonce())) {
                redirect.addFlashAttribute("erro", "Retorno do Bling inválido. Tente conectar novamente.");
                return "redirect:/integracoes";
            }
            conectarBling.concluir(empresaId, code);
            redirect.addFlashAttribute("sucesso", "Bling conectado com sucesso.");
        } catch (FalhaBlingException e) {
            redirect.addFlashAttribute("erro", "Não foi possível concluir a conexão com o Bling.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("erro", "Erro inesperado ao conectar o Bling. Tente novamente.");
        }
        return "redirect:/integracoes";
    }
}
