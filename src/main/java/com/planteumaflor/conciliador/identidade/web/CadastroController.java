package com.planteumaflor.conciliador.identidade.web;

import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario;
import com.planteumaflor.conciliador.identidade.application.CadastrarEmpresaEUsuario.CadastrarEmpresaCommand;
import com.planteumaflor.conciliador.identidade.application.EmailJaCadastradoException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    CadastroController(CadastrarEmpresaEUsuario cadastrar) {
        this.cadastrar = cadastrar;
    }

    @GetMapping
    String formulario(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "auth/cadastro";
    }

    @PostMapping
    String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult erros) {
        if (!form.senhasConferem()) {
            erros.rejectValue("confirmarSenha", "senha.divergente", "As senhas não conferem.");
        }
        if (erros.hasErrors()) {
            return "auth/cadastro";   // 200 com o formulário e os erros
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
            return "auth/cadastro";
        }

        // Passo 2: redireciona para o login. (Auto-login pós-cadastro é a próxima
        // evolução prevista na tela 02 §8.)
        return "redirect:/entrar?cadastro";
    }
}
