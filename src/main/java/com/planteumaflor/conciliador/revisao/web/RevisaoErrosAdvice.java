package com.planteumaflor.conciliador.revisao.web;

import com.planteumaflor.conciliador.revisao.application.ConflitoDeVersao;
import com.planteumaflor.conciliador.revisao.application.RecursoNaoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Tradução de erros da revisão (Backend §10): recurso de outra empresa → 404,
 * conflito de versão → 409, violação de regra de domínio/validação → 422. Cada
 * handler devolve um fragmento de alerta para o HTMX exibir.
 */
@ControllerAdvice(assignableTypes = RevisaoController.class)
class RevisaoErrosAdvice {

    @ExceptionHandler(RecursoNaoEncontrado.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String naoEncontrado(RecursoNaoEncontrado ex, Model model) {
        return erro(model, ex.getMessage());
    }

    @ExceptionHandler(ConflitoDeVersao.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflito(ConflitoDeVersao ex, Model model) {
        return erro(model, ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    String regraInvalida(RuntimeException ex, Model model) {
        return erro(model, ex.getMessage());
    }

    private String erro(Model model, String mensagem) {
        model.addAttribute("erro", mensagem);
        return "revisao/fragments :: erro";
    }
}
