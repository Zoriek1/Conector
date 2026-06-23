package com.planteumaflor.conciliador.identidade.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renderiza a tela de login (tela 01).
 *
 * Só o GET é nosso: o POST de credenciais (/entrar) é processado pelo filtro do
 * Spring Security, não por um método aqui.
 */
@Controller
class LoginController {

    @GetMapping("/entrar")
    String entrar() {
        return "auth/login";
    }
}
