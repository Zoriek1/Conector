# Tela 01 — Login

> **Rota pública:** `GET /entrar`  
> **Processamento:** `POST /entrar` pelo Spring Security  
> **Template:** `templates/auth/login.html`  
> **Controller:** `identidade.web.LoginController`
> **Segurança:** `identidade.security`
> **Domínio:** `identidade.domain`

## 1. Função

Autenticar o único acesso operacional da empresa e criar uma sessão segura. A
tela não consulta dados financeiros nem seleciona empresa; a empresa é obtida do
usuário autenticado.

Após sucesso:

- empresa sem Pluggy ativo → `/onboarding`;
- empresa configurada → `/inicio`;
- requisição interna salva → retorna à URL original, se segura.

## 2. Contrato visual

Conteúdo:

- marca Plante Uma Flor/Conciliador;
- campo e-mail;
- campo senha;
- opção futura de recuperação de senha;
- botão `Entrar`;
- link `Criar conta` quando o cadastro estiver aberto;
- mensagem genérica de credenciais inválidas;
- mensagem de logout ou sessão expirada.

Desktop usa card central com largura limitada. Mobile ocupa a largura disponível
sem reduzir os alvos de toque.

## 3. Classes e interfaces

O controller fica em `identidade.web`. `UsuarioPrincipal`, handlers e a
implementação de `UserDetailsService` ficam em `identidade.security`; `Usuario` e
seu repositório-porta ficam em `identidade.domain`.

```java
@Controller
@RequestMapping("/entrar")
final class LoginController {
    @GetMapping
    String exibirLogin(/* parâmetros seguros de feedback */, Model model) {
        return "auth/login";
    }
}
```

O controller não implementa interface e não estende `BaseController`. O POST de
credenciais é responsabilidade do filtro de autenticação do Spring Security.

Tipos envolvidos:

| Tipo | Forma | Responsabilidade |
|---|---|---|
| `LoginController` | classe concreta | renderizar a página |
| `UsuarioDetailsService` | implementa `UserDetailsService` | carregar usuário por e-mail normalizado |
| `UsuarioPrincipal` | implementa `UserDetails` | expor `usuarioId`, `empresaId` e authorities |
| `LoginSuccessHandler` | implementa `AuthenticationSuccessHandler` | escolher onboarding, início ou saved request |
| `LoginFailureHandler` | implementa `AuthenticationFailureHandler` | redirecionar com erro genérico |
| `SecurityConfig` | configuração | declarar form login, sessão, CSRF e logout |

## 4. Composição e extensão

- `login.html` compõe `layout/auth.html` com `th:replace`.
- Não existe herança entre templates.
- O formulário usa componentes compartilhados de campo, erro e botão.
- Não criar uma interface própria para autenticação apenas para encapsular Spring
  Security; a fronteira já é `UserDetailsService`.

## 5. Encapsulamento

- O controller não recebe `empresaId`.
- `UsuarioDetailsService` retorna somente dados necessários para autenticação.
- Hash de senha nunca entra em view model, log ou sessão.
- A mensagem é igual para e-mail inexistente, senha errada e usuário desativado.
- A URL de retorno aceita somente caminho interno validado.
- O identificador da sessão é renovado após login.

## 6. Formulário e validação

Campos enviados segundo a configuração do Spring Security:

```text
username = e-mail normalizado
password = senha informada
_csrf    = token CSRF
```

Não validar existência do e-mail no navegador. Botão entra em estado ocupado e
impede duplo envio.

## 7. HTTP e erros

| Situação | Resultado |
|---|---|
| GET sem sessão | 200 + login |
| GET com sessão válida | redirect `/inicio` ou `/onboarding` |
| credenciais válidas | 302 para destino seguro |
| credenciais inválidas | 302 `/entrar?erro` |
| usuário desativado | mesma resposta de credencial inválida |
| sessão expirada | 302 `/entrar?expirada` |

Login usa submit de página completa, não HTMX. Isso simplifica saved requests,
cookies e redirecionamentos de segurança.

## 8. Segurança

- Rate limit por IP e e-mail normalizado.
- Cookie `HttpOnly`, `Secure` em produção e `SameSite=Lax`.
- CSRF habilitado.
- Sem indicação de qual empresa pertence ao e-mail antes da autenticação.
- Logout é POST com CSRF.
- Depois de sucessivas falhas, aplicar atraso progressivo sem revelar bloqueio.

## 9. Acessibilidade

- Um único `h1`.
- Labels visíveis.
- Foco inicial no e-mail, exceto quando houver mensagem de erro.
- Erro anunciado por `role="alert"`.
- Autocomplete: `username` e `current-password`.
- Mostrar/ocultar senha preserva o rótulo acessível.

## 10. Testes

- renderiza sem sessão;
- redireciona usuário já autenticado;
- autentica e cria principal com `empresaId` correto;
- envia empresa sem integração ao onboarding;
- mantém saved request interno;
- rejeita redirect externo;
- não diferencia e-mail inexistente e senha inválida;
- renova ID da sessão;
- exige CSRF no logout.

## 11. Critérios de aceite

- Nenhum dado de outra empresa é consultado durante login.
- Credenciais inválidas não revelam existência do usuário.
- Sessão autenticada contém identidade e tenant imutáveis.
- Usuário configurado chega ao início; usuário novo chega ao onboarding.
