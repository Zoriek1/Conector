# Tela 07 — Integrações

> **Rota autenticada:** `GET /integracoes`  
> **Template:** `templates/integracoes/index.html`  
> **Página agregadora:** `integracoes.web`, `integracoes.query`
> **Operações:** `pluggy.application`, `bling.application`, `ingest.application`
> **Bordas externas:** `pluggy.client`, `bling.client`

## 1. Função

Exibir e administrar as integrações da empresa: Pluggy, contas bancárias
descobertas e Bling. A tela permite conectar, reconectar, sincronizar e remover
conexões sem expor credenciais.

Contas bancárias continuam sendo filtros globais nas telas operacionais; esta
tela administra sua disponibilidade e saúde.

## 2. Interfaces de aplicação

`ConsultarIntegracoes` e sua projeção agregada ficam em `integracoes.query`.
Commands de cada fornecedor ficam em `pluggy.application` ou
`bling.application`. A página agregadora fica em `integracoes.web`; callbacks e
webhooks específicos permanecem no `web` da feature correspondente.

```java
public interface ConsultarIntegracoes {
    IntegracoesView consultar(EmpresaId empresaId);
}

public interface GerenciarPluggy {
    ConexaoPluggyIniciada iniciar(EmpresaId empresaId);
    void confirmarContas(EmpresaId empresaId, ConfirmarContasCommand comando);
    SincronizacaoId sincronizar(EmpresaId empresaId, UUID integracaoId);
    void desconectar(EmpresaId empresaId, DesconectarIntegracaoCommand comando);
}

public interface GerenciarBling {
    URI iniciarOAuth(EmpresaId empresaId);
    void desconectar(EmpresaId empresaId, DesconectarIntegracaoCommand comando);
}
```

O contrato do fornecedor fica atrás de adapters. Controllers dependem destas
interfaces, não de `PluggyClient` ou `BlingClient` diretamente.

## 3. Controllers

```text
IntegracoesController
├── GET  /integracoes
├── GET  /integracoes/status
└── GET  /integracoes/contas

PluggyController
├── POST /integracoes/pluggy/conectar
├── POST /integracoes/pluggy/{id}/sincronizar
├── POST /integracoes/pluggy/{id}/contas
└── POST /integracoes/pluggy/{id}/desconectar

BlingOAuthController
├── POST /integracoes/bling/conectar
├── GET  /integracoes/bling/retorno
└── POST /integracoes/bling/{id}/desconectar
```

Controllers são classes concretas, não estendem `BaseController` e não
implementam protocolo OAuth ou regra de token.

Webhooks externos usam controller separado, autenticação/assinatura própria e
não compartilham endpoints de sessão com esta tela.

## 4. View models

```java
public record IntegracoesView(
    IntegracaoView pluggy,
    List<ContaBancariaView> contas,
    IntegracaoView bling,
    boolean sincronizacaoEmAndamento
) {}

public record IntegracaoView(
    UUID id,
    String nome,
    StatusVisual status,
    String mensagem,
    Instant conectadoEm,
    Instant ultimaSincronizacao,
    List<AcaoView> acoes
) {}
```

Tokens, client secrets, payloads e erros brutos nunca aparecem nos view models.

## 5. Composição de templates

- `integracoes/index.html` compõe `layout/app.html`.
- `integracoes/pluggy-card.html` mostra conexão agregadora.
- `integracoes/contas.html` mostra contas descobertas.
- `integracoes/bling-card.html` mostra OAuth e validade operacional.
- `integracoes/status.html` atualiza sincronização.
- `integracoes/modal-desconectar.html` confirma impacto.

Não há herança. Cards compartilham fragments visuais, mas preservam ações
específicas de cada integração.

## 6. Encapsulamento

- `empresaId` vem da sessão.
- Toda integração é carregada por `id + empresa_id`.
- O browser não recebe access token nem refresh token.
- Credenciais globais da aplicação permanecem na configuração do servidor.
- Conta bancária não pode ser vinculada manualmente a outra empresa.
- Status é derivado no backend.
- Erro persistido é sanitizado antes de virar mensagem.
- Remover integração não executa `DELETE CASCADE` no histórico financeiro.

## 7. Conta bancária

Cada linha/card mostra:

- instituição;
- nome/tipo da conta;
- identificador mascarado;
- status;
- última sincronização;
- quantidade recente importada;
- habilitada ou pausada para ingestão.

Pausar ingestão é diferente de remover conexão. A ação preserva histórico e
deve ser idempotente.

## 8. Conectar e reconectar Pluggy

Usa o mesmo adapter do onboarding:

1. POST autenticado solicita sessão de conexão.
2. Backend gera dados efêmeros vinculados à empresa.
3. Fragmento ativa o widget com JavaScript local.
4. Conclusão é confirmada no backend.
5. Lista de contas e status são atualizados via HTMX.

Reconectar atualiza a integração existente quando o fornecedor confirmar a mesma
origem; não cria duplicatas silenciosas.

## 9. Conectar Bling

OAuth ocorre por redirect. `state` é assinado, temporário e vinculado à empresa.
O callback troca o código no servidor e retorna para `/integracoes`.

Validade do token pode ser exibida como estado operacional, nunca como valor do
token.

## 10. Sincronizar agora

- POST apenas agenda o trabalho.
- Resposta muda card para `SINCRONIZANDO`.
- Polling consulta status até terminal.
- Duplo clique não cria duas execuções concorrentes.
- Limite de frequência é aplicado no backend.
- Falha de uma conta não apaga sucesso das demais.

## 11. Desconectar

Modal informa:

- novos dados deixarão de ser importados;
- histórico existente será preservado;
- eventos pendentes podem bloquear a remoção;
- reconexão será necessária para retomar.

Command inclui ID e version, mas não empresa. Backend revalida pendências e
tenant. Operação pode ser recusada com 422 e orientação específica.

## 12. HTMX e erros

- Status e contas são fragments independentes.
- Durante sync, polling para em `ATIVA` ou `REQUER_ATENCAO`.
- 409 recarrega o card atualizado.
- 422 substitui modal/card com justificativa.
- 404 é igual para inexistente e outro tenant.
- OAuth usa navegação completa; não tentar encaixá-lo em swap HTMX.

## 13. Responsividade e acessibilidade

- Desktop: Pluggy e Bling lado a lado; contas abaixo.
- Mobile: cards empilhados e contas como lista.
- Status combina texto, ícone e cor.
- Modal de remoção gerencia foco.
- Progresso assíncrono usa `aria-live` sem anunciar cada poll.
- Botões informam quando abrem fluxo externo.

## 14. Testes

- lista apenas integrações da empresa;
- conexão criada recebe tenant da sessão;
- ID de outro tenant retorna 404;
- token nunca aparece no HTML;
- callback OAuth valida state, empresa, expiração e uso único;
- reconexão é idempotente;
- sincronização dupla é coalescida/rejeitada;
- desconexão preserva histórico;
- pendência bloqueia remoção quando necessário;
- polling termina corretamente;
- conta pausada não participa do próximo ingest.

## 15. Critérios de aceite

- Usuário entende a saúde das integrações sem acessar logs.
- Conectar/reconectar não exige editar ambiente por empresa.
- Contas podem ser filtradas e pausadas sem separar tabelas financeiras.
- Nenhuma credencial é exposta ao frontend.
- Ações respeitam tenant e são idempotentes.
