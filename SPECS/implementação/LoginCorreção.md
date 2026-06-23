# Correções — Tela 01 Login

> Contrato: [`SPECS/TELAS/01-Login.md`](../TELAS/01-Login.md)
> Código atual: `identidade.web.LoginController`, `identidade.security.*`,
> `config.SecurityConfig`, `templates/auth/login.html`.
> Cobertura estimada: **~70%**.

## Já atende ✅
- `GET /entrar` + `POST /entrar` (filtro do Spring), `UsuarioDetailsService`,
  `UsuarioPrincipal` (`usuarioId`/`empresaId`), `LoginSuccessHandler`
  (onboarding vs início), form login + sessão + CSRF.
- Mensagem genérica de erro, link "Criar conta", `h1` único, labels,
  `autocomplete=username/current-password`, `role="alert"`, logout POST com CSRF.

## Pendentes

### 🔧 Agora (desvios de contrato baratos)
- [ ] **Saved-request: voltar à URL originalmente pedida** (§1, §5, §10).
  - Onde: `identidade.security.LoginSuccessHandler`.
  - Como: estender `SavedRequestAwareAuthenticationSuccessHandler` OU consultar
    `HttpSessionRequestCache`. Se houver saved request **interno e seguro**, ir
    para ele; senão, aplicar a regra atual (onboarding vs início).
  - Teste: acessar `/inicio` sem sessão → login → após autenticar volta para
    `/inicio`; rejeitar `?redirect=http://externo` (redirect externo).
- [ ] **Usuário já autenticado que abre `/entrar` é redirecionado** (§7).
  - Onde: `LoginController.entrar(...)`.
  - Como: se `Authentication` presente e autenticada, `redirect:` para
    início/onboarding em vez de renderizar o form.
  - Teste: `@WithUserDetails`/`user(principal)` em `GET /entrar` → 3xx.
- [ ] **Mensagem de sessão expirada** (`/entrar?expirada`) (§2, §7).
  - Onde: `templates/auth/login.html` (já trato `?erro`/`?saiu`/`?cadastro`) +
    configurar `invalidSessionUrl("/entrar?expirada")` no `sessionManagement`.
  - Teste: requisição com sessão inválida → redirect `/entrar?expirada`.

### 💅 Hardening
- [ ] **Rate limit por IP + e-mail** e **atraso progressivo** após falhas (§8).
  - Onde: um filtro/`AuthenticationFailureHandler` + cache (ex.: Bucket4j ou
    contagem em memória/Redis). Não revelar bloqueio.
- [ ] **Mostrar/ocultar senha** preservando rótulo acessível (§9).
  - Onde: `templates/auth/login.html` + `static/js/app.js`.
- [ ] **Cookie `Secure` em produção** (§8).
  - Onde: `application.yml` (`server.servlet.session.cookie.secure: true` por
    profile `prod`) — `HttpOnly` e `SameSite=Lax` já são default.
- [ ] **`LoginFailureHandler` dedicado** (§3) — hoje uso `.failureUrl("/entrar?erro")`,
  comportamento equivalente; criar a classe só se precisar de lógica extra.

### 🧱 Estrutural (alinhar com o spec, opcional)
- [ ] Compor **`layout/auth.html`** (§4) em vez do fragmento `fragments/layout :: head`.
  - Impacto: login.html e cadastro.html passam a usar o mesmo layout de auth.

## Testes a adicionar (§10)
- [ ] renderiza sem sessão (200 + form);
- [ ] redireciona usuário já autenticado;
- [ ] mantém saved request interno; rejeita redirect externo;
- [ ] mesma resposta para e-mail inexistente, senha errada e usuário desativado;
- [ ] renova o ID da sessão após login (session fixation);
- [ ] exige CSRF no logout.

> Já cobertos hoje: autentica e cria principal com `empresaId`; empresa sem
> Pluggy vai ao onboarding (ver `IdentidadeIntegrationTest`/`OnboardingIntegrationTest`).
