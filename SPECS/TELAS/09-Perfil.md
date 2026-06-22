# Tela 09 — Perfil

> **Rota autenticada:** `GET /perfil`  
> **Template:** `templates/perfil/index.html`  
> **Pacotes:** `identidade`, `empresa`

## 1. Função

Exibir os dados da empresa e do acesso, permitir alteração segura de senha e
encerrar a sessão. Não existe troca de empresa ou administração de múltiplos
usuários no v1.

## 2. Interfaces de aplicação

```java
public interface ConsultarPerfil {
    PerfilView consultar(UsuarioId usuarioId, EmpresaId empresaId);
}

public interface AlterarSenha {
    void executar(UsuarioId usuarioId, EmpresaId empresaId, AlterarSenhaCommand comando);
}

public interface AtualizarPerfil {
    PerfilView executar(UsuarioId usuarioId, EmpresaId empresaId, AtualizarPerfilCommand comando);
}

public interface EncerrarOutrasSessoes {
    void executar(UsuarioId usuarioId, String sessaoAtualId);
}
```

`AtualizarPerfil` só será exposto para campos realmente editáveis. Não criar um
update genérico de `Usuario` ou `Empresa`.

## 3. Controller

```java
@Controller
@RequestMapping("/perfil")
final class PerfilController {
    private final ConsultarPerfil consultar;
    private final AlterarSenha alterarSenha;
    private final AtualizarPerfil atualizarPerfil;
    private final EncerrarOutrasSessoes encerrarSessoes;
}
```

Rotas:

| Método | Rota | Função |
|---|---|---|
| GET | `/perfil` | página |
| POST | `/perfil/dados` | atualizar campos permitidos |
| POST | `/perfil/senha` | alterar senha |
| POST | `/perfil/sessoes/encerrar-outras` | invalidar outras sessões |
| POST | `/sair` | logout pelo Spring Security |

Controller não estende classe base, não recebe `Usuario`/`Empresa` por binding e
não codifica senha diretamente.

## 4. View models e commands

```java
public record PerfilView(
    String nomeEmpresa,
    String cnpjMascarado,
    String nomeResponsavel,
    String email,
    Instant senhaAlteradaEm,
    Instant acessoAtualEm
) {}

public record AlterarSenhaCommand(
    char[] senhaAtual,
    char[] novaSenha,
    char[] confirmacao
) {}
```

Não expor hash, IDs de tenant editáveis, tokens de integração ou detalhes de
outras sessões.

## 5. Composição de templates

- `perfil/index.html` compõe `layout/app.html`.
- `perfil/empresa.html` mostra dados empresariais.
- `perfil/acesso.html` mostra responsável e e-mail.
- `perfil/form-senha.html` altera senha.
- `perfil/sessoes.html` permite encerrar outras sessões.
- logout reutiliza fragmento global de navegação.

Não há herança. Cada formulário chama um caso de uso específico.

## 6. Encapsulamento

- `usuarioId` e `empresaId` vêm do principal autenticado.
- Formulário não escolhe usuário ou empresa.
- Atualização usa allowlist de campos.
- E-mail, se editável no futuro, exige fluxo próprio de verificação.
- Senha atual é verificada pelo `PasswordEncoder`.
- Nova senha nunca retorna em view model ou log.
- Alterar senha pode invalidar outras sessões.
- Dados financeiros não são carregados nesta tela.

## 7. Alteração de senha

1. Usuário informa senha atual, nova e confirmação.
2. Controller valida formato básico.
3. Caso de uso recarrega o usuário autenticado.
4. Verifica senha atual.
5. Aplica política e impede reutilização quando houver suporte.
6. Persiste novo hash e timestamp.
7. Invalida outras sessões.
8. Mantém ou renova a sessão atual conforme configuração.

Erro de senha atual usa mensagem controlada e não preserva campos preenchidos.

## 8. Dados da empresa

No v1, nome e CNPJ podem ser somente leitura para evitar alteração sem impacto
definido sobre integrações e auditoria. Nome do responsável pode ser editável.

Se edição empresarial for habilitada, deve existir command explícito com regras
de domínio; não usar binding direto sobre `Empresa`.

## 9. Logout e sessões

- Logout é processado pelo Spring Security.
- Usa POST e CSRF.
- Invalida sessão no servidor e cookie no navegador.
- `Encerrar outras sessões` preserva a sessão atual e registra auditoria.
- A tela não mostra tokens ou IDs brutos de sessão.

## 10. HTTP e erros

| Situação | Resultado |
|---|---|
| perfil válido | 200 |
| senha atual incorreta | 422 no formulário |
| nova senha inválida | 422 no formulário |
| versão concorrente | 409 + dados atuais |
| sessão expirada | redirect login |
| sucesso em senha | fragmento limpo + confirmação |

HTMX pode atualizar cada seção de forma independente. Logout continua como
navegação completa.

## 11. Segurança

- CSRF em todas as mutações.
- Reautenticação pode ser exigida para alterações sensíveis.
- Autocomplete: `current-password` e `new-password`.
- Sem `empresaId` ou `usuarioId` em inputs ocultos como autoridade.
- Resposta não revela detalhes de sessões encerradas.
- Rate limit para tentativa de senha atual.
- Auditoria de alteração de senha sem registrar seu conteúdo.

## 12. Responsividade e acessibilidade

- Desktop: seções em cards com largura legível.
- Mobile: uma coluna e botões de largura adequada.
- Erros associados aos campos.
- Confirmação de encerramento de sessões gerencia foco.
- Campos de senha suportam gerenciadores de senha.
- Ação de logout é clara, mas não compete com salvar alterações.

## 13. Testes

- perfil usa usuário e empresa da sessão;
- tentativa de enviar outro ID é ignorada/rejeitada;
- senha atual obrigatória e verificada;
- nova senha é armazenada como hash;
- senhas não aparecem na resposta/log;
- outras sessões são invalidadas;
- sessão atual segue política definida;
- logout exige CSRF e invalida sessão;
- atualização aceita somente campos permitidos;
- fragmentos retornam erros 422 corretamente.

## 14. Critérios de aceite

- Usuário vê somente os dados da própria empresa e acesso.
- Alteração de senha não expõe nem persiste texto puro.
- Não existe seletor de empresa no v1.
- Cada ação chama interface específica e mantém entidades encapsuladas.
- Logout e encerramento de sessões funcionam no desktop e mobile.

