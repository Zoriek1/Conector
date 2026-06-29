# Correções — Tela 04 Início

> Contrato: [`SPECS/TELAS/04-Inicio.md`](../TELAS/04-Inicio.md)
> Código atual: `inicio.web.InicioController`, `inicio.query.*`,
> `templates/inicio/index.html`.
> Cobertura estimada: **~70%** — núcleo entregue em 2026-06-27.

## Já atende ✅
- `GET /inicio` autenticado; lê `empresaId` do `UsuarioPrincipal`.
- `ConsultarInicio` + `InicioView` em `inicio.query`: contagens por estado
  no banco (`emRevisao`, `falhas`, `aguardandoApi`, `aguardandoOfx`,
  `conciliadas`), escopadas por `empresaId`.
- `IntegracaoStatusView`: nome, status, última sincronização, última falha,
  falhas consecutivas por integração (Cora e Pluggy).
- `AtividadeView`: últimas transações com data, conta, descrição, valor,
  direção e estado.
- Template `inicio/index.html` com os 5 cards e faixa de integrações.

## Pendentes

### ⬜ HTMX / fragmentos
- [ ] `GET /inicio/resumo` como fragmento HTMX para atualização parcial dos
  cards sem recarregar o layout; exibir "Atualizado em hh:mm".
- [ ] Sem polling permanente quando estável — atualizar só sob demanda.
- [ ] Ação **"Sincronizar agora"** (caso de uso próprio, fora de
  `ConsultarInicio`) com feedback imediato via fragmento.

### ⬜ Empty state e erros
- [ ] Empresa sem conectores ativos → `fragments/empty-state.html` com CTA
  "Conectar bancos".
- [ ] Banner acionável quando integração requer atenção ("Reconecte o Cora").

### ⬜ Layout
- [ ] Compor `layout/app.html` (app shell com sidebar desktop + nav mobile);
  hoje usa `navbar` mínima.

### ♿ Acessibilidade (§11)
- [ ] Contadores com rótulo completo; cards clicáveis como links reais;
  status texto+ícone; `aria-live="polite"` na atualização;
  ordem visual = ordem do DOM.

## Testes a adicionar
- [ ] Totais filtrados por empresa; empresas com dados iguais → contagens
  independentes; links produzem filtros válidos; integração com falha mostra
  ação; empresa sem transações direciona ao onboarding; fragmento HTMX não
  devolve layout completo; query não carrega entidades completas.
