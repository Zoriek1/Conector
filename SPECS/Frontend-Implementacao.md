# Conciliador — Frontend: detalhamento de implementação

> **Status:** guia de implementação v1 · concretiza Frontend.md e as telas 01–09.
> **Objetivo:** transformar os contratos das telas em decisões concretas de
> template, layout, fragments, HTMX e assets — sem reabrir regra de negócio.
> **Stack:** Thymeleaf + HTMX (WebJar) + Bootstrap (WebJar) + CSS próprio. Sem
> SPA, sem API JSON, sem Node (Frontend §3).

---

## 1. Layouts e composição

Usar **Thymeleaf Layout Dialect** ou `th:replace`/`th:insert` puros. As telas
exigem composição sem herança de template (cada tela "compõe" um layout). Dois
layouts:

```text
templates/layout/
├── app.html     # área autenticada: app shell (sidebar + nav mobile + header)
└── auth.html    # área pública: card central (login, cadastro)
```

`app.html` define os fragments-âncora que cada página preenche:

```html
<!-- layout/app.html (esqueleto) -->
<!DOCTYPE html>
<html lang="pt-br" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="_csrf" th:content="${_csrf.token}">
  <meta name="_csrf_header" th:content="${_csrf.headerName}">
  <link rel="stylesheet" th:href="@{/webjars/bootstrap/css/bootstrap.min.css}">
  <link rel="stylesheet" th:href="@{/css/tokens.css}">
  <link rel="stylesheet" th:href="@{/css/app.css}">
  <link rel="stylesheet" th:href="@{/css/components.css}">
  <title th:text="${tituloPagina} + ' · Conciliador'">Conciliador</title>
</head>
<body>
  <div th:replace="~{fragments/navegacao :: sidebar}"></div>
  <main class="conteudo">
    <header th:replace="~{fragments/cabecalho :: header(${tituloPagina}, ${descricao})}"></header>
    <section layout:fragment="conteudo">...</section>
  </main>
  <nav th:replace="~{fragments/navegacao :: navmobile}"></nav>
  <div th:replace="~{fragments/feedback :: toasts}"></div>
  <script th:src="@{/webjars/htmx.org/dist/htmx.min.js}"></script>
  <script th:src="@{/js/app.js}"></script>
</body>
</html>
```

`@{...}` resolve o context path `/page` automaticamente — **nunca** concatenar
`/page` à mão (Frontend §4.2).

---

## 2. Árvore de templates (Frontend §20.2 + telas)

```text
templates/
├── layout/        app.html · auth.html
├── fragments/
│   ├── navegacao.html      (sidebar, navmobile)
│   ├── cabecalho.html      (header da página, breadcrumbs)
│   ├── feedback.html       (alertas, toasts, aria-live)
│   ├── filtros.html        (base de filtros reutilizável)
│   ├── paginacao.html
│   ├── badge-estado.html   (vocabulário de negócio, não enum)
│   ├── confianca.html      (Alta/Média/Baixa + %)
│   ├── filtro-conta.html   (seletor de conta bancária)
│   ├── empty-state.html
│   ├── skeleton.html
│   ├── modal-confirmacao.html
│   └── painel-lateral.html
├── auth/          login.html · cadastro.html
├── onboarding/    index.html · etapas.html · pluggy.html · contas.html · bling.html · sincronizacao.html
├── inicio/        index.html · resumo.html · integracoes.html · atividade.html
├── revisao/       index.html · filtros.html · resumo.html · fila.html · linha.html · card.html · detalhe.html · form-classificacao.html · form-match.html
├── transacoes/    index.html · filtros.html · lista.html · linha.html · card.html · detalhe.html
├── integracoes/   index.html · pluggy-card.html · contas.html · bling-card.html · status.html · modal-desconectar.html
├── ofx/           index.html
└── perfil/        index.html
```

Regra (Frontend §13): **componentes de negócio recebem view model pronto.** O
template não calcula classe, taxa, permissão ou transição — isso vem do
`query`/`application`. `revisao/detalhe.html` renderiza apenas as ações de
`AcoesPermitidasView`; ocultar botão **não** substitui a validação no backend
(tela 05 §5).

---

## 3. Assets estáticos (Frontend §20.2)

```text
static/
├── css/
│   ├── tokens.css       # custom properties: cores da marca, espaçamento, tipografia
│   ├── app.css          # app shell, grid, layout
│   └── components.css    # badges, painel, cards, indicador de confiança
├── js/
│   ├── app.js           # CSRF nos POSTs HTMX, busy state, foco pós-swap
│   └── pluggy-connect.js# único JS de fornecedor (widget Pluggy)
└── img/
```

`tokens.css` define o tema substituível (Frontend §17 — paleta/logo oficiais
ainda pendentes):

```css
:root {
  --cor-marca: #2e7d32;        /* verde Plante Uma Flor (placeholder) */
  --cor-credito: #1b5e20;
  --cor-debito: #b71c1c;        /* vermelho só p/ débito/destrutivo */
  --cor-sucesso: var(--cor-marca);
  --num-tabular: "tnum" 1;      /* numerais tabulares em valores */
}
```

---

## 4. Contrato HTMX concreto (Frontend §14 + telas)

`app.js` cuida do que se repete em toda mutação:

```js
// CSRF em todo request HTMX (header lido dos <meta> do layout)
document.body.addEventListener('htmx:configRequest', (e) => {
  const t = document.querySelector('meta[name="_csrf"]').content;
  const h = document.querySelector('meta[name="_csrf_header"]').content;
  e.detail.headers[h] = t;
});
```

Convenções por tipo de resposta (idênticas em todas as telas):

| Situação | Backend responde | HTMX faz |
|---|---|---|
| Navegação comum | página completa (layout) | full load |
| Requisição HTMX de sucesso | só o fragment + `hx-swap-oob` para resumo | troca alvo + out-of-band |
| Validação | **422** + fragment do formulário com erros | substitui só o form |
| Conflito de `version` | **409** + item recarregado | recarrega item, avisa alteração |
| Recurso ausente/cross-tenant | **404** genérico | fecha painel, mensagem genérica |
| Sessão expirada | header de redirect HTMX para `/entrar` | redireciona |

Padrões obrigatórios:
- Todo `POST` carrega CSRF e usa **busy state** (`hx-disabled-elt`) para impedir
  duplo envio.
- Filtros `GET` usam `hx-push-url` (URL compartilhável e sobrevive a refresh) e
  **debounce só na busca textual** (`hx-trigger="keyup changed delay:300ms"`).
- **Polling** só em sincronização ativa (`hx-trigger="every 3s"`) e **para** ao
  chegar a estado terminal (o fragment para de emitir o atributo, ou usa
  `HX-Trigger` para `htmx:abort`).
- Fragments têm **elemento raiz estável e IDs previsíveis** (Frontend §14). O
  backend nunca devolve JS arbitrário.

Login e OAuth **não** são HTMX: usam submit/redirect de página completa
(telas 01 §7, 03 §9, 07 §12) — simplifica saved request, cookies e `state`.

---

## 5. App shell responsivo (Frontend §5, §16)

- **Desktop:** sidebar fixa (Início, Revisão [badge], Transações, Lotes OFX,
  Integrações, Perfil) + área fluida. Rodapé da sidebar: empresa, e-mail, Sair.
- **Mobile:** header compacto + nav inferior com 4 destinos (Início, Revisão,
  Transações, Menu). Menu secundário: Integrações, OFX, Perfil, Sair. **Sem
  dependência de hover.**
- Breakpoints pelos tokens do Bootstrap; **sem** lógica de servidor por
  user-agent. Alvos de toque ≥ 44×44px.
- O badge de pendências da Revisão vem de uma projeção contada no banco (não
  carregar agregados), atualizado junto com `/inicio/resumo`.

A escolha tabela (desktop) vs cards (mobile) é **CSS responsivo** sobre o mesmo
fragment quando possível (tela 05 §6: `fila.html` escolhe `linha.html`/`card.html`
por CSS), evitando duplicar dados no DOM.

---

## 6. Detalhe das telas mais densas

### 6.1 Revisão (tela central, 05)

- `GET /revisao` → página; `GET /revisao/fila` → lista filtrada; `GET
  /revisao/resumo` → contadores; `GET /revisao/{id}` → painel.
- Mutações: `aprovar`, `classificar`, `match`, `ofx`, `retry` — todas `POST` com
  `id + version + CSRF`. Resposta de sucesso troca a **linha** e envia o
  **resumo** por `hx-swap-oob`.
- O painel lateral abre **sem perder filtros/paginação** (tela 05 §5). Após ação:
  foco vai ao próximo item ou à confirmação, anúncio em `aria-live`.
- Progressive enhancement: tudo funciona sem HTMX (303 + reload) — tela 05 §11.

### 6.2 Transações (06) e filtros consolidados

- Lista única de todas as contas; **filtro de conta é recorte**, não tela
  separada. `FiltroTransacao` (record) com allowlist de ordenação e limites de
  página/período.
- Somente leitura; detalhe abre painel; documento **mascarado** já na projeção
  (tela 06 §7).

### 6.3 Onboarding (03) e Integrações (07)

- Fluxo Pluggy: cada empresa usa o **seu próprio Meu Pluggy**. Primeiro a empresa
  **informa as credenciais** (clientId/clientSecret), que o backend guarda por
  empresa criptografadas; depois `POST conectar` → fragment com **token efêmero**
  (gerado com as credenciais daquela empresa) → `pluggy-connect.js` abre o widget
  → backend confirma e persiste contas com `empresa_id`. O contrato do widget fica
  isolado em `PluggyConnectAdapter` (tela 03 §8). As credenciais nunca vão ao
  navegador.
- Fluxo Bling: `state` assinado de uso único → redirect de consentimento →
  callback troca código no servidor → tokens por empresa.
- Status por polling moderado em `GET /onboarding/status` (ver AUDITORIA §3 sobre
  padronizar essa rota vs `/integracoes/status`).
- Tokens **nunca** vão para `localStorage`/`sessionStorage`/HTML (Frontend §19).

---

## 7. Estados obrigatórios de tela (Frontend §21)

Toda página de dados implementa, via fragments compartilhados: carregando
(`skeleton`), vazia-primeira-vez, vazia-por-filtro, sucesso, erro recuperável,
sem permissão, sessão expirada, alterado concorrentemente, integração
desconectada. **Empty state diz o porquê e oferece a próxima ação** (ex.: sem
Pluggy → botão "Conectar bancos").

---

## 8. Acessibilidade (Frontend §18) — checklist de implementação

- HTML semântico antes de ARIA; um único `h1` por página.
- Todo campo com `label` visível; mensagens por `aria-describedby`.
- Foco: retorna a local coerente após swap HTMX; painel/modal prendem foco e
  devolvem ao acionador; Escape fecha painel.
- `aria-live="polite"` para resultado de ação e progresso (sem reanunciar a
  página inteira).
- Estado **nunca** só por cor: crédito/débito = cor + ícone + texto; confiança =
  texto (Alta/Média/Baixa) + %; badge de estado em vocabulário de negócio.
- Contraste WCAG AA; fluxos principais operáveis por teclado; `aria-sort` na
  ordenação das tabelas.

---

## 9. Segurança no frontend (Frontend §19)

- O navegador **nunca** escolhe/envia `empresa_id` como autoridade; o backend
  consulta sempre por `id + empresa da sessão`.
- IDs de recurso podem aparecer na rota; cross-tenant → **404 genérico**.
- CSRF em todas as mutações (incl. logout via POST).
- Thymeleaf escapa descrição/documento/dados bancários por padrão.
- Sem segredo/token persistido no navegador; dados sensíveis mascarados nas
  listas, revelados só no detalhe quando necessário.

---

## 10. Testes de frontend (Frontend §22)

- **MVC/templates (`MockMvc`):** página completa vs fragment HTMX; filtros
  preservados na query string; paginação/ordenação; 422; 409; redirect de sessão
  expirada; CSRF em todos os POSTs; 404 cross-tenant.
- **E2E (navegador):** cadastro→onboarding; Pluggy simulado→1ª sync;
  login→revisão→aprovação; classificação com erro e correção; OFX gerar→confirmar;
  acesso cruzado entre duas empresas; fluxos mobile. Clients externos simulados.

---

## 11. Definition of done do frontend v1 (Frontend §23)

- [ ] Cadastro cria sessão e abre onboarding (sem novo login).
- [ ] Usuário só vê dados da própria empresa; cross-tenant → 404 genérico.
- [ ] Todas as contas em **uma lista consolidada** com filtro.
- [ ] Aprovar na revisão **não** recarrega a página inteira; filtros/paginação
      sobrevivem ao abrir/fechar detalhe.
- [ ] Desktop = tabela; mobile = cards sem rolagem horizontal obrigatória.
- [ ] Todo POST HTMX com CSRF e sem duplo envio.
- [ ] Sessão expirada redireciona ao login.
- [ ] Componentes operáveis por teclado e não dependem só de cor.
- [ ] Nenhum segredo/token de integração no navegador.

---

## 12. Pendências de frontend (Frontend §24)

Continuam abertas e **não bloqueiam** o esqueleto: cadastro público vs convite,
verificação de e-mail/recuperação de senha, logo/paleta/tipografia oficiais, se o
Bling é obrigatório no onboarding, dados exatos do cadastro (CNPJ), política de
desconexão com eventos pendentes, contrato final do widget Pluggy. Até os ativos
oficiais chegarem, o tema vive em `tokens.css` (custom properties substituíveis).
