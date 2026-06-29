# Correções — Tela 03 Onboarding

> Contrato: [`SPECS/TELAS/03-Onboarding.md`](../TELAS/03-Onboarding.md)
> Código atual: `onboarding.*`, `pluggy.*`, `cora.*`, `conta.*`,
> `templates/onboarding/index.html`.
> Cobertura estimada: **~55%** — credenciais reais, contas e Cora entregues
> em 2026-06-27.

## Já atende ✅
- `GET /onboarding`, etapa **derivada** do banco, `empresaId` da sessão.
- Porta/adaptador Pluggy isolando o core (`PluggyConnectPort` +
  `FakePluggyConnectAdapter` para testes + **`PluggyGatewayAdapter` real**).
- Credenciais do Meu Pluggy **por empresa, criptografadas no banco** via
  `CriptoService` + migration V9.
- `ContaBancaria` persistida (domínio + `ContaBancariaJpaRepository` + V8).
- `ContaBancariaService`: descoberta, ativação e pausa de contas Pluggy.
- Integração Cora completa: `CadastrarCredencialCoraService`, `CoraSyncScheduler`,
  `IntegracaoCoraSaudeService`, `IntegracaoCoraJpaRepository`, V5 e V7.
- `EtapaOnboarding` com 3 estágios operacionais: `INTEGRACOES_PENDENTES`,
  `CONTAS_PENDENTES`, `CONCLUIDO`.
- `OnboardingService` derivando a etapa do banco.
- Template `onboarding/index.html` atualizado.

## Pendentes

### ⬜ Etapas e tela completa
- [ ] Ampliar `EtapaOnboarding` para as 7 etapas do spec (§2): `CONTA_CRIADA`,
  `PLUGGY_PENDENTE`, `CONTAS_PENDENTES`, `BLING_PENDENTE`, `SINCRONIZANDO`,
  `CONCLUIDO`, `REQUER_ATENCAO`. Ajustar `OnboardingService` (derivação).
- [ ] `OnboardingView` rico (§4): etapa + `IntegracaoView` Pluggy/Bling +
  contas + sincronização + `podeConcluir`. Substituir retorno só-`EtapaOnboarding`.

### ⬜ Bling OAuth
- [ ] `BlingOAuthController` (`POST /integracoes/bling/conectar`,
  `GET /integracoes/bling/retorno`), `state` assinado de uso único,
  token por empresa (migration `bling_oauth_token`).

### ⬜ Primeira sincronização
- [ ] `IniciarPrimeiraSincronizacao` + `GET /onboarding/status` com
  **polling HTMX** que para em estado terminal.
- [ ] `POST /onboarding/sincronizar`. Depende do worker de ingest.

### ⬜ UI / HTMX
- [ ] Quebrar `onboarding/index.html` em fragments (§6): `etapas`, `pluggy`,
  `contas`, `bling`, `sincronizacao`, substituídos por HTMX.
- [ ] Compor `layout/app.html` em modo reduzido.
- [ ] Stepper horizontal (desktop) / lista vertical (mobile);
  `aria-live="polite"` no progresso; estado com texto+ícone, nunca só cor (§13).

### ⬜ Erros e segurança (§11, §12)
- [ ] Estados de erro: cancelar Pluggy, callback inválido, consentimento
  expirado, nenhuma conta, Bling recusado, falha na 1ª sync, 404 cross-tenant.
- [ ] `state` de callback assinado, uso único, expiração; callback repetido
  idempotente.

## Testes a adicionar
- [ ] Retoma onboarding no ponto correto; callback state inválido/expirado
  falha; callback repetido é idempotente; duas empresas sem mistura; polling
  para em terminal; tokens não aparecem em HTML/log; 1ª sync fora da requisição.

> Já cobertos (`OnboardingIntegrationTest`): etapa derivada do banco,
> conectar persiste/avança/idempotente, POST autenticado redireciona, login
> com integração ativa vai para início.
