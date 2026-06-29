package com.planteumaflor.conciliador.identidade.security;

import com.planteumaflor.conciliador.identidade.application.EncerrarOutrasSessoes;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Encerra as demais sessões do usuário via {@link SessionRegistry} (tela 09 §9).
 *
 * Localiza o principal pelo {@code usuarioId} (sem depender de identidade de
 * objeto), expira todas as suas sessões e preserva a atual. Marcar como expirada
 * faz o {@code ConcurrentSessionFilter} invalidar a sessão no próximo acesso.
 */
@Service
class SessionRegistryEncerrarOutrasSessoes implements EncerrarOutrasSessoes {

    private final SessionRegistry sessionRegistry;

    SessionRegistryEncerrarOutrasSessoes(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void executar(UUID usuarioId, String sessaoAtualId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UsuarioPrincipal up && up.usuarioId().equals(usuarioId)) {
                for (SessionInformation sessao : sessionRegistry.getAllSessions(principal, false)) {
                    if (!sessao.getSessionId().equals(sessaoAtualId)) {
                        sessao.expireNow();
                    }
                }
            }
        }
    }
}
