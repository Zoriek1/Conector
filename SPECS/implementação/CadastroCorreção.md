# Correções — Tela 02 Cadastro

> Contrato: [`SPECS/TELAS/02-Cadastro.md`](../TELAS/02-Cadastro.md)
> Código atual: `identidade.web.CadastroController`/`CadastroForm`,
> `identidade.application.CadastroService`, `templates/auth/cadastro.html`.
> Cobertura estimada: **~65%**.

## Já atende ✅
- `CadastrarEmpresaEUsuario` + command + `CadastroRealizado`; `CadastroService`
  `@Transactional` (empresa+usuário atômico); unicidade por constraint; senha só
  como hash; normalização de e-mail no servidor; sem mass-assignment; CSRF.

## Pendentes

### 🔧 Agora (desvios reais de contrato)
- [ ] **Auto-login pós-cadastro → entra autenticado no onboarding** (§8.7, §13).
  - Hoje: `return "redirect:/entrar?cadastro"`.
  - Onde: `CadastroController.cadastrar(...)`.
  - Como: após o caso de uso, autenticar programaticamente e persistir o contexto
    na sessão:
    ```java
    var principal = /* carregar UsuarioPrincipal do usuário criado */;
    var auth = UsernamePasswordAuthenticationToken.authenticated(
        principal, null, principal.getAuthorities());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    return "redirect:/onboarding";
    ```
    (injetar `HttpSessionSecurityContextRepository` ou usar `SecurityContextRepository`).
  - Teste: POST `/cadastro` válido → 3xx `/onboarding` **e** sessão autenticada.
- [ ] **Não reapresentar a senha em erro** (§8, §12) — *gap de segurança*.
  - Hoje: `th:field="*{senha}"` repopula o campo no re-render.
  - Onde: `templates/auth/cadastro.html`.
  - Como: para `senha`/`confirmarSenha`, **não** usar `th:field` (que escreve
    `value`); usar `name="senha"` sem `value`, ou limpar no controller antes de
    re-renderizar.
  - Teste: POST inválido → corpo do form **não** contém o valor da senha.
- [ ] **Retornar HTTP 422 em validação e e-mail duplicado** (§9).
  - Hoje: retorno a view com status 200.
  - Onde: `CadastroController` — anotar o handler/branch de erro com
    `@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)` ou retornar
    `ResponseEntity`/`ModelAndView` com status 422.
  - Teste: POST inválido → status 422 + form com erros; e-mail duplicado → 422.

### ⏳ Depois / 💅 Hardening
- [ ] **Unicidade de CNPJ** (se definido obrigatório) → constraint + tradução 422 (§9).
- [ ] **Página de erro genérica + correlation ID** para erro inesperado (§9).
  - Onde: `@ControllerAdvice` central (servirá a todas as telas).
- [ ] **`char[] senha` + limpeza** após uso (§3, §4) — hoje uso `String`.
- [ ] **`aria-describedby`** ligando mensagens aos campos (§11).
- [ ] **Aceite de termos** quando os termos existirem (§2).

### 🧱 Estrutural
- [ ] Compor `layout/auth.html` (§6) e reusar fragments de campo/erro/requisitos de senha.
- [ ] (Opcional) `CadastroForm` como `record` — hoje é classe por causa do `th:field`.

## Testes a adicionar (§12)
- [ ] **testes do controller** (faltam — só testei o caso de uso): GET form,
  POST válido → onboarding autenticado, POST inválido → 422 sem senha no corpo,
  e-mail duplicado → 422, ignora campos extras de tenant/papel, exige CSRF.

> Já cobertos hoje (em `IdentidadeIntegrationTest`): cria atomicamente, normaliza
> e-mail, e-mail duplicado não cria 2ª empresa, senha persistida é hash.
