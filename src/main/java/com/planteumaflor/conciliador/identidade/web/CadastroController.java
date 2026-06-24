package com.planteumaflor.conciliador.identidade.web;

import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.EmailJaCadastradoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Cadastro de empresa + usuário (tela 02).
 *
 * O controller só: valida o formato (Bean Validation), confere senha x
 * confirmação, chama o caso de uso e traduz o erro de e-mail duplicado. Não
 * acessa repositório nem implementa regra de unicidade.
 */
@Controller
@RequestMapping("/cadastro")
class CadastroController {

    private final CadastrarEmpresaEUsuario cadastrar;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    CadastroController(CadastrarEmpresaEUsuario cadastrar,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.cadastrar = cadastrar;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping
    String formulario(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "auth/cadastro";
    }

    @PostMapping
    ModelAndView cadastrar(@Valid @ModelAttribute("form") CadastroForm form,
                           BindingResult erros,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (!form.senhasConferem()) {
            erros.rejectValue("confirmarSenha", "senha.divergente", "As senhas não conferem.");
        }
        if (erros.hasErrors()) {
            return formularioInvalido();
        }

        try {
            cadastrar.executar(new CadastrarEmpresaCommand(
                    form.getNomeEmpresa(),
                    form.getCnpj(),
                    form.getNomeResponsavel(),
                    form.getEmail(),
                    form.getSenha()));
        } catch (EmailJaCadastradoException e) {
            erros.rejectValue("email", "email.duplicado",
                    "Não foi possível concluir o cadastro com esse e-mail.");
            return formularioInvalido();
        }

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(form.getEmail(), form.getSenha()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return new ModelAndView("redirect:/onboarding");
    }

    private ModelAndView formularioInvalido() {
        ModelAndView modelAndView = new ModelAndView("auth/cadastro");
        modelAndView.setStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        return modelAndView;
    }
}
