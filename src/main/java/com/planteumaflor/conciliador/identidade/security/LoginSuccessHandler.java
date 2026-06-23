package com.planteumaflor.conciliador.identidade.security;

import com.planteumaflor.conciliador.onboarding.application.ConsultarOnboarding;
import com.planteumaflor.conciliador.onboarding.domain.EtapaOnboarding;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Decide para onde mandar o usuário após o login (tela 01):
 * empresa sem Pluggy ativo → /onboarding; configurada → /inicio.
 *
 * Usa {@code request.getContextPath()} para respeitar o context-path {@code /page}.
 */
@Component
class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ConsultarOnboarding onboarding;
    private final RequestCache requestCache;

    LoginSuccessHandler(ConsultarOnboarding onboarding, RequestCache requestCache) {
        this.onboarding = onboarding;
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            requestCache.removeRequest(request, response);
            String destinoSalvo = destinoInternoSeguro(request, savedRequest);
            if (destinoSalvo != null) {
                response.sendRedirect(destinoSalvo);
                return;
            }
        }

        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        EtapaOnboarding etapa = onboarding.etapaAtual(principal.empresaId());

        String destino = (etapa == EtapaOnboarding.CONCLUIDO) ? "/inicio" : "/onboarding";
        response.sendRedirect(request.getContextPath() + destino);
    }

    private String destinoInternoSeguro(HttpServletRequest request, SavedRequest savedRequest) {
        if (!"GET".equalsIgnoreCase(savedRequest.getMethod())) {
            return null;
        }

        try {
            URI destino = new URI(savedRequest.getRedirectUrl());
            if (destino.getUserInfo() != null || destino.getFragment() != null) {
                return null;
            }
            if (destino.isAbsolute() && !mesmaOrigem(request, destino)) {
                return null;
            }

            String caminho = destino.getRawPath();
            if (caminho == null || !caminho.startsWith("/") || caminho.startsWith("//")) {
                return null;
            }

            String contexto = request.getContextPath();
            if (!contexto.isEmpty() && !caminho.equals(contexto) && !caminho.startsWith(contexto + "/")) {
                return null;
            }
            return destino.getRawQuery() == null ? caminho : caminho + "?" + destino.getRawQuery();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private boolean mesmaOrigem(HttpServletRequest request, URI destino) {
        int portaDestino = portaEfetiva(destino.getScheme(), destino.getPort());
        int portaRequisicao = portaEfetiva(request.getScheme(), request.getServerPort());
        return destino.getScheme().equalsIgnoreCase(request.getScheme())
                && destino.getHost() != null
                && destino.getHost().equalsIgnoreCase(request.getServerName())
                && portaDestino == portaRequisicao;
    }

    private int portaEfetiva(String esquema, int porta) {
        if (porta >= 0) {
            return porta;
        }
        return "https".equalsIgnoreCase(esquema) ? 443 : 80;
    }
}
