package com.planteumaflor.conciliador.identidade.web;

import com.planteumaflor.conciliador.identidade.application.AlterarSenha;
import com.planteumaflor.conciliador.identidade.application.AlterarSenha.AlterarSenhaCommand;
import com.planteumaflor.conciliador.identidade.application.AtualizarPerfil;
import com.planteumaflor.conciliador.identidade.application.AtualizarPerfil.AtualizarDadosCommand;
import com.planteumaflor.conciliador.identidade.application.ConsultarPerfil;
import com.planteumaflor.conciliador.identidade.application.ConsultarPerfil.PerfilView;
import com.planteumaflor.conciliador.identidade.application.EncerrarOutrasSessoes;
import com.planteumaflor.conciliador.identidade.application.SenhaAtualIncorretaException;
import com.planteumaflor.conciliador.identidade.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;

/**
 * Perfil da empresa e do acesso (tela 09).
 *
 * O controller obtém {@code usuarioId}/{@code empresaId} SEMPRE do principal
 * autenticado — nunca de inputs do navegador. Só valida formato e traduz erros;
 * regra e codificação de senha ficam nos casos de uso. Logout segue pelo Spring
 * Security em {@code POST /sair}.
 */
@Controller
@RequestMapping("/perfil")
class PerfilController {

    private final ConsultarPerfil consultar;
    private final AlterarSenha alterarSenha;
    private final AtualizarPerfil atualizarPerfil;
    private final EncerrarOutrasSessoes encerrarSessoes;

    PerfilController(ConsultarPerfil consultar,
                     AlterarSenha alterarSenha,
                     AtualizarPerfil atualizarPerfil,
                     EncerrarOutrasSessoes encerrarSessoes) {
        this.consultar = consultar;
        this.alterarSenha = alterarSenha;
        this.atualizarPerfil = atualizarPerfil;
        this.encerrarSessoes = encerrarSessoes;
    }

    @GetMapping
    String pagina(@AuthenticationPrincipal UsuarioPrincipal principal,
                  HttpServletRequest request, Model model) {
        PerfilView perfil = consultar.consultar(principal.usuarioId(), principal.empresaId());
        preencherModelo(model, perfil, request);
        if (!model.containsAttribute("formDados")) {
            AtualizarDadosForm dados = new AtualizarDadosForm();
            dados.setNomeResponsavel(perfil.nomeResponsavel());
            model.addAttribute("formDados", dados);
        }
        model.addAttribute("formSenha", new AlterarSenhaForm());
        return "perfil/index";
    }

    @PostMapping("/dados")
    Object atualizarDados(@AuthenticationPrincipal UsuarioPrincipal principal,
                          @Valid @ModelAttribute("formDados") AtualizarDadosForm form,
                          BindingResult erros,
                          HttpServletRequest request, Model model) {
        if (erros.hasErrors()) {
            return paginaComErro(principal, request, model);
        }
        atualizarPerfil.executar(principal.usuarioId(), principal.empresaId(),
                new AtualizarDadosCommand(form.getNomeResponsavel()));
        return "redirect:/perfil?dados";
    }

    @PostMapping("/senha")
    Object alterarSenha(@AuthenticationPrincipal UsuarioPrincipal principal,
                        @Valid @ModelAttribute("formSenha") AlterarSenhaForm form,
                        BindingResult erros,
                        HttpServletRequest request, Model model) {
        String mensagem = null;
        if (erros.hasErrors()) {
            mensagem = erros.getAllErrors().get(0).getDefaultMessage();
        } else if (!form.novaSenhaConfere()) {
            mensagem = "As senhas não conferem.";
        }

        if (mensagem == null) {
            try {
                alterarSenha.executar(principal.usuarioId(), principal.empresaId(),
                        new AlterarSenhaCommand(form.getSenhaAtual(), form.getNovaSenha(), form.getConfirmacao()));
            } catch (SenhaAtualIncorretaException e) {
                mensagem = "A senha atual está incorreta.";
            }
        }

        if (mensagem != null) {
            return paginaSenhaInvalida(principal, request, model, mensagem);
        }
        return "redirect:/perfil?senha";
    }

    @PostMapping("/sessoes/encerrar-outras")
    String encerrarOutrasSessoes(@AuthenticationPrincipal UsuarioPrincipal principal,
                                 HttpServletRequest request) {
        encerrarSessoes.executar(principal.usuarioId(), request.getSession().getId());
        return "redirect:/perfil?sessoes";
    }

    /** Recarrega a página em caso de erro de validação de dados (status 422). */
    private ModelAndView paginaComErro(UsuarioPrincipal principal, HttpServletRequest request, Model model) {
        PerfilView perfil = consultar.consultar(principal.usuarioId(), principal.empresaId());
        preencherModelo(model, perfil, request);
        model.addAttribute("formSenha", new AlterarSenhaForm());
        return resposta422();
    }

    /**
     * Recarrega a página com erro na seção de senha (status 422). Os campos de
     * senha NÃO são preservados (tela 09 §7): sempre um form limpo.
     */
    private ModelAndView paginaSenhaInvalida(UsuarioPrincipal principal, HttpServletRequest request,
                                             Model model, String mensagemSenhaAtual) {
        PerfilView perfil = consultar.consultar(principal.usuarioId(), principal.empresaId());
        preencherModelo(model, perfil, request);
        AtualizarDadosForm dados = new AtualizarDadosForm();
        dados.setNomeResponsavel(perfil.nomeResponsavel());
        model.addAttribute("formDados", dados);
        model.addAttribute("formSenha", new AlterarSenhaForm());
        if (mensagemSenhaAtual != null) {
            model.addAttribute("erroSenha", mensagemSenhaAtual);
        }
        return resposta422();
    }

    private void preencherModelo(Model model, PerfilView perfil, HttpServletRequest request) {
        model.addAttribute("perfil", perfil);
        model.addAttribute("email", perfil.email());
        model.addAttribute("acessoAtualEm",
                Instant.ofEpochMilli(request.getSession().getCreationTime()));
    }

    private ModelAndView resposta422() {
        ModelAndView mv = new ModelAndView("perfil/index");
        mv.setStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        return mv;
    }
}
