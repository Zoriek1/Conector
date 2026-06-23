# Correções — Tela 03 Onboarding

> Contrato: [`SPECS/TELAS/03-Onboarding.md`](../TELAS/03-Onboarding.md)
> Código atual: `onboarding.*`, `pluggy.*` (com **fake**), `templates/onboarding/index.html`.
> Cobertura estimada: **~25%** — hoje é uma **fundação fake**, por decisão
> ("fake agora, real depois").

## Já atende ✅
- `GET /onboarding`, etapa **derivada** do banco, `empresaId` da sessão.
- **Porta/adaptador** isolando o Pluggy (`PluggyConnectPort` +
  `FakePluggyConnectAdapter`) — o real entra trocando o bean.
- Conexão (fake) idempotente persiste `IntegracaoPluggy` ATIVA.

## Pendentes

### ⏳ Depois — completar o fluxo (a maior parte do spec)
- [ ] **Informar credenciais do Meu Pluggy por empresa** (§7, §8.1).
  - Migration nova: colunas cifradas (`meu_pluggy_client_id`,
    `meu_pluggy_client_secret`) em `integracao_pluggy` ou tabela própria.
  - Componente de cifragem (usar `CRIPTO_KEY` já previsto no `application.yml`).
  - Caso de uso `ConfigurarCredenciaisPluggy` + form na tela.
- [ ] **Expandir `EtapaOnboarding`** para as 7 etapas (§2): `CONTA_CRIADA`,
  `PLUGGY_PENDENTE`, `CONTAS_PENDENTES`, `BLING_PENDENTE`, `SINCRONIZANDO`,
  `CONCLUIDO`, `REQUER_ATENCAO`. Ajustar `OnboardingService` (derivação).
- [ ] **Confirmar contas descobertas** (§8.8–8.9): entidade `ContaBancaria`
  (`pluggy.domain`) + migration, `ConfirmarContasPluggy`, lista na tela.
- [ ] **Conectar Bling (OAuth)** (§9): `bling.web.BlingOAuthController`
  (`POST /integracoes/bling/conectar`, `GET /integracoes/bling/retorno`),
  `state` assinado de uso único, token por empresa (migration `bling_oauth_token`).
- [ ] **Primeira sincronização** (§10): `IniciarPrimeiraSincronizacao`,
  `GET /onboarding/status` com **polling HTMX** que para em estado terminal;
  `POST /onboarding/sincronizar`. Depende do worker de ingest (passo posterior).
- [ ] **`OnboardingView` rico** (§4): etapa + `IntegracaoView` Pluggy/Bling +
  contas + sincronização + `podeConcluir`. Substituir o retorno só-`EtapaOnboarding`.

### ⏳ Adapter real do Pluggy
- [ ] Implementar `PluggyConnectAdapterReal` (widget + API) e marcá-lo
  `@Primary`/`@Profile`, aposentando o fake (mantê-lo para testes). Ver
  [`SPECS/Bling-API-v3.md`](../Bling-API-v3.md) e docs do Pluggy.

### 🧱 UI / HTMX
- [ ] Quebrar `onboarding/index.html` em fragments (§6): `etapas`, `pluggy`,
  `contas`, `bling`, `sincronizacao`, substituídos por HTMX.
- [ ] Compor `layout/app.html` em modo reduzido.
- [ ] Stepper horizontal (desktop) / lista vertical (mobile); `aria-live="polite"`
  no progresso; estado com texto+ícone, nunca só cor (§13).

### 🔒 Erros e segurança (§11, §12)
- [ ] Estados de erro: cancelar Pluggy, callback inválido, consentimento expirado,
  nenhuma conta, Bling recusado, falha na 1ª sync, 404 cross-tenant.
- [ ] `state` de callback assinado, uso único, expiração; callback repetido idempotente.

## Testes a adicionar (§14)
- [ ] retoma onboarding no ponto correto; callback state inválido/expirado falha;
  callback repetido é idempotente; duas empresas sem mistura; polling para em
  terminal; tokens não aparecem em HTML/log; 1ª sync roda fora da requisição.

> Já cobertos hoje (`OnboardingIntegrationTest`): etapa derivada do banco,
> conectar persiste/avança/idempotente, POST autenticado redireciona, login com
> Pluggy ativo vai para início.
