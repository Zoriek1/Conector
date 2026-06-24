# Correções — Tela 04 Início

> Contrato: [`SPECS/TELAS/04-Inicio.md`](../TELAS/04-Inicio.md)
> Código atual: `inicio.web.InicioController` + `templates/inicio/index.html`
> (**placeholder**).
> Cobertura estimada: **~5%**.

## Já atende ✅
- `GET /inicio` autenticado; lê `empresaId` do `UsuarioPrincipal`; renderiza uma
  página mínima (prova de sessão/tenant).

## ⚠️ Dependência
A tela real conta **transações por estado** — depende do **passo 4** (tabela
`transacao` + agregado). Sem `transacao`, os cards não têm o que contar. Por isso
a maior parte abaixo é ⏳ **depois do passo 4**.

## Pendentes

### ⏳ Depois (núcleo da tela, requer `transacao`)
- [ ] **`ConsultarInicio` + `InicioView`** em `inicio.query` (§2): projeção de
  leitura, contagens **no banco** (não carregar agregados), escopadas por `empresaId`.
- [ ] **5 cards de resumo** com links filtrados (§7):
  - Em revisão → `/revisao?estado=EM_REVISAO`
  - Falhas → `/revisao?estado=FALHA`
  - Aguardando API → `/transacoes?estado=AGUARDANDO_ESCRITA_API`
  - Aguardando OFX → `/ofx/lotes`
  - Conciliadas → `/transacoes?estado=CONCILIADO`
- [ ] **`InicioController` com `ConsultarInicio`** (§3): `GET /inicio` (página) e
  `GET /inicio/resumo` (fragmento HTMX). Hoje o controller só monta o placeholder.
- [ ] **Faixa de integrações** (Pluggy/Bling + última sincronização) com texto
  acionável ("Reconecte o Cora"), não só ícone (§1, §9).
- [ ] **Atividade recente** (últimas transações/ações) (§1, §6).

### 🧱 UI / HTMX / estados
- [ ] Compor **`layout/app.html`** (app shell com sidebar desktop + nav mobile) —
  hoje uso só a `navbar` mínima.
- [ ] `GET /inicio/resumo` atualiza cards/integrações via HTMX; "Atualizado em";
  sem polling permanente quando estável (§8).
- [ ] **Empty state**: empresa sem Pluggy → `fragments/empty-state.html` com
  "Conectar bancos"; banner acionável quando integração requer atenção (§9).
- [ ] Ação **"Sincronizar agora"** (caso de uso próprio, fora de `ConsultarInicio`) (§7).

### ♿ Acessibilidade (§11)
- [ ] Contadores com rótulo completo; cards clicáveis como links reais; status
  texto+ícone; `aria-live="polite"` na atualização; ordem visual = ordem do DOM.

## Testes a adicionar (§12)
- [ ] totais filtrados por empresa; empresas com dados iguais → contagens
  independentes; links produzem filtros válidos; integração com falha mostra ação;
  página vazia direciona ao onboarding; fragmento HTMX não devolve layout completo;
  query não carrega entidades completas.

## Substituir o placeholder
- [ ] Remover o conteúdo "Você está autenticado…" de `templates/inicio/index.html`
  quando a tela real entrar.
