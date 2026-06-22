# Tela 02 — Cadastro

> **Rotas públicas:** `GET /cadastro`, `POST /cadastro`  
> **Template:** `templates/auth/cadastro.html`  
> **Pacote principal:** `com.planteumaflor.conciliador.identidade`

## 1. Função

Criar atomicamente uma empresa e seu único usuário no v1. A tela coleta apenas
identidade e acesso; Pluggy e Bling são conectados depois no onboarding.

## 2. Contrato visual

Campos:

- nome da empresa;
- CNPJ, se definido como obrigatório;
- nome do responsável;
- e-mail;
- senha;
- confirmação da senha;
- aceite de termos, quando disponíveis.

A página explica que integrações bancárias serão configuradas na etapa seguinte.

## 3. Interface de aplicação

```java
public interface CadastrarEmpresaEUsuario {
    CadastroRealizado executar(CadastrarEmpresaCommand comando);
}

public record CadastrarEmpresaCommand(
    String nomeEmpresa,
    String cnpj,
    String nomeResponsavel,
    String email,
    char[] senha
) {}

public record CadastroRealizado(UUID empresaId, UUID usuarioId) {}
```

`CadastroService` implementa `CadastrarEmpresaEUsuario`, tem visibilidade de
pacote e coordena `Empresa`, `Usuario`, repositórios e `PasswordEncoder` dentro de
uma única transação.

O command não contém `empresaId`; ele será criado pelo caso de uso.

## 4. Controller

```java
@Controller
@RequestMapping("/cadastro")
final class CadastroController {
    private final CadastrarEmpresaEUsuario cadastrar;

    // GET: renderiza formulário
    // POST: valida, executa caso de uso, autentica e redireciona
}
```

O controller:

- não estende classe base;
- não implementa regra de senha ou unicidade;
- não acessa repositórios;
- mapeia `CadastroForm` para o command;
- limpa a senha mutável depois do uso quando tecnicamente viável.

## 5. View models

```java
public record CadastroForm(
    String nomeEmpresa,
    String cnpj,
    String nomeResponsavel,
    String email,
    String senha,
    String confirmarSenha,
    boolean aceitouTermos
) {}
```

`CadastroForm` pertence à borda web e contém anotações de Bean Validation. Não é
entidade nem atravessa para o domínio.

## 6. Composição e extensão

- `cadastro.html` compõe `layout/auth.html`.
- Reutiliza fragments de campo, resumo de erros e requisitos de senha.
- Não existe `BaseFormController`.
- A interface `CadastrarEmpresaEUsuario` é a fronteira entre web e aplicação;
  detalhes JPA permanecem encapsulados na implementação.

## 7. Invariantes

- empresa e usuário são criados na mesma transação;
- e-mail normalizado é único;
- senha e confirmação devem coincidir;
- senha atende à política definida;
- usuário referencia exatamente a empresa recém-criada;
- falha em qualquer gravação desfaz todo o cadastro;
- senha nunca é persistida em texto puro.

Unicidade é garantida por constraint no banco. A verificação antecipada serve
somente para uma mensagem melhor.

## 8. Fluxo

1. GET renderiza formulário vazio.
2. POST valida formato.
3. Controller cria command.
4. Caso de uso normaliza e valida invariantes.
5. Cria `Empresa` e `Usuario` com senha codificada.
6. Commit.
7. Aplicação autentica o usuário recém-criado.
8. Redirect `/onboarding`.

O formulário usa submit completo. HTMX não traz benefício relevante neste fluxo.

## 9. Erros

| Situação | Resultado |
|---|---|
| validação de campos | 422 + formulário com erros |
| e-mail já cadastrado | 422 + mensagem controlada |
| CNPJ duplicado, se único | 422 + orientação |
| erro inesperado | página genérica + correlation ID |
| cadastro concluído | 302 `/onboarding` |

Não manter senha preenchida depois de erro.

## 10. Segurança e privacidade

- CSRF obrigatório.
- Rate limit de cadastro.
- Não aceitar `empresaId`, papel ou status do formulário.
- Não fazer mass assignment do form para entidade.
- Normalizar e-mail no servidor.
- Escapar todos os valores reapresentados.
- Não enviar senha a ferramentas de analytics ou observabilidade.

## 11. Acessibilidade e responsividade

- Ordem lógica de campos em uma coluna.
- Desktop pode agrupar empresa e responsável visualmente, sem mudar a ordem do
  teclado.
- Mensagens associadas por `aria-describedby`.
- Requisitos da senha são textuais, não apenas cores.
- Mobile usa teclado apropriado para e-mail e CNPJ.

## 12. Testes

- cria empresa e usuário atomicamente;
- rollback quando criação do usuário falha;
- normaliza e-mail;
- constraint impede duplicidade concorrente;
- senha persistida é hash;
- não reapresenta senha em erro;
- autentica depois do cadastro;
- redireciona ao onboarding;
- ignora campos extras de tenant/papel;
- exige CSRF.

## 13. Critérios de aceite

- Um cadastro bem-sucedido cria exatamente uma empresa e um usuário.
- Nenhuma integração externa é chamada dentro da transação de cadastro.
- O usuário entra autenticado no onboarding.
- Entidades não são expostas diretamente à camada web.

