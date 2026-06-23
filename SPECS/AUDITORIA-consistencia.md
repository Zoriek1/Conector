# Conciliador — Auditoria de consistência das specs

> **Status:** auditoria v1 · revisão cruzada de README, ARQUITETURA, Backend,
> Frontend e as 9 telas.
> **Objetivo:** localizar divergências entre os documentos antes do início da
> implementação e indicar a correção recomendada.

A maior parte das divergências tem a mesma origem: **ARQUITETURA-conciliador.md
foi escrita para um v0 de empresa única e HTTP Basic**, e depois Backend.md e
Frontend.md evoluíram para **multiempresa com form login**, declarando precedência
sobre o documento geral. O Backend e o Frontend marcam essa precedência, mas a
ARQUITETURA e o README **não foram reescritos**, então continuam contendo o
modelo antigo. As telas (01–09) seguem o modelo novo.

Legenda de severidade:
- 🔴 **Bloqueante** — divergência no modelo de dados / contrato que precisa de
  decisão única antes de codar.
- 🟡 **Atenção** — divergência real, porém com precedência já declarada; basta
  alinhar o texto.
- 🔵 **Menor** — naming/rota/enriquecimento sem contradição de regra.

---

## 1. Quadro-resumo

| # | Sev | Tema | Onde diverge | Decisão recomendada |
|---|-----|------|--------------|---------------------|
| 1 | 🔴 | `empresa_id` ausente no modelo de dados | ARQUITETURA §4 não tem `empresa_id`; Backend §5.0/§7 exige em todas as tabelas | Backend vence. ARQUITETURA §4 precisa de `empresa_id` + FKs. |
| 2 | 🔴 | Unicidade da transação | ARQUITETURA: `UNIQUE(pluggy_transaction_id)`; Backend: `UNIQUE(empresa_id, pluggy_transaction_id)` | Backend vence (multitenant). |
| 3 | 🔴 | Estados da `Transacao` | ARQUITETURA §6 vai `classificado → escrito_api`; Backend §5.3 insere `AGUARDANDO_ESCRITA_API`; tela 04 usa esse estado | Backend vence. Diagrama da ARQUITETURA está defasado. |
| 4 | 🟡 | `tentativas` / `erro_ultimo` | ARQUITETURA §4 em `transacao`; Backend §7.2 move para `outbox_bling` | Backend vence (já documentado como refinamento). |
| 5 | 🟡 | Autenticação | ARQUITETURA §11.1: "ao menos HTTP Basic"; Backend §1/§11 e Frontend: form login + sessão | Backend/Frontend vencem (precedência declarada). |
| 6 | 🟡 | Empresa única vs multiempresa | ARQUITETURA inteira pressupõe 1 empresa; Backend §1 é multiempresa | Backend vence. |
| 7 | 🟡 | Enum `ClasseTransacao` | ARQUITETURA §4 não lista `PRO_LABORE`; Backend §5.2 lista | Backend vence; ARQUITETURA §9 já cita pró-labore "como classe à parte". |
| 8 | 🔵 | Rota de status do onboarding | tela 03 §10: `/onboarding/status`; Frontend §15: `/integracoes/status` | Padronizar (ver §3). |
| 9 | 🔵 | Confirmar contas Pluggy | tela 03: `POST /integracoes/pluggy/confirmar-contas`; tela 07: `POST /integracoes/pluggy/{id}/contas` | Padronizar a forma da rota. |
| 10 | 🔵 | `/revisao/resumo` | tela 05 define; Backend §10 e Frontend §15 não listam | Adicionar nas duas tabelas de rota. |
| 11 | 🔵 | Bootstrap no stack | Frontend §3 adiciona Bootstrap (WebJar); ARQUITETURA §11.7 e README citam só Thymeleaf/HTMX | Enriquecimento; alinhar README/ARQUITETURA. |
| 12 | ✅ | Premissa de custo Pluggy | — | **Resolvido:** cada empresa usa o seu próprio Meu Pluggy (free, R$0/empresa). Credenciais por empresa no banco, criptografadas. Ver §4. |
| 13 | 🔵 | Volume "~6 req/dia" | ARQUITETURA §3 assume 1 empresa; multiempresa escala com nº de empresas | Reescrever a estimativa por empresa. |

---

## 2. Divergências bloqueantes (detalhe)

### 2.1 🔴 `empresa_id` no modelo de dados (#1, #2)

A tabela `transacao` em **ARQUITETURA §4** não tem `empresa_id` e usa
`pluggy_transaction_id TEXT NOT NULL UNIQUE`. O **Backend §5.0** torna `Empresa`
a raiz de isolamento, exige `empresa_id` em todas as tabelas compartilhadas e
define a unicidade como `UNIQUE (empresa_id, pluggy_transaction_id)` (§6.1).

Isso não é cosmético: a constraint global `UNIQUE(pluggy_transaction_id)`
**quebra o isolamento** se dois tenants pudessem ver o mesmo id (não devem, mas a
constraint certa é a composta) e, principalmente, o `empresa_id` é o eixo de toda
query escopada exigida pelas telas (404 cross-tenant).

**Correção:** reescrever ARQUITETURA §4 para incluir `empresa_id UUID NOT NULL
REFERENCES empresa(id)`, trocar a unicidade para a composta e ajustar os índices
(`idx_transacao_match` deve começar por `empresa_id`).

### 2.2 🔴 Estado `AGUARDANDO_ESCRITA_API` (#3)

ARQUITETURA §6 modela `classificado ──► escrito_api ──► conciliado`. O Backend
§5.3 insere um estado intermediário **`AGUARDANDO_ESCRITA_API`** (item já no
outbox, ainda não confirmado pelo Bling), e a tela **04-Inicio §7** liga um card
direto a `?estado=AGUARDANDO_ESCRITA_API`. O conjunto de estados do Backend é o
correto porque o padrão outbox precisa distinguir "enfileirado" de "confirmado".

**Correção:** atualizar o diagrama da ARQUITETURA §6 para os 8 estados do Backend
§5.3.

---

## 3. Rotas a padronizar (#8, #9, #10)

Há três pequenas divergências de contrato HTTP entre as telas e o Frontend.md.
Recomendação de fonte única (escolher e replicar nas duas tabelas — Backend §10 e
Frontend §15):

| Necessidade | Recomendado | Observação |
|---|---|---|
| Polling de progresso no onboarding | `GET /onboarding/status` | Mantém o status do onboarding sob a feature `onboarding`. `/integracoes/status` fica para a tela 07. |
| Confirmar contas Pluggy | `POST /integracoes/pluggy/{id}/contas` | Forma por-id é mais coerente com as demais ações da tela 07; ajustar tela 03. |
| Resumo/contadores da revisão | `GET /revisao/resumo` | Já usado pela tela 05; falta listar em Backend §10 e Frontend §15. |

Nenhuma dessas muda regra de negócio — é alinhamento de texto.

---

## 4. Pontos que a auditoria reabre (não são erro de texto)

- **Custo do Pluggy (#12) — RESOLVIDO.** Decisão do dono: **cada empresa usa o
  seu próprio Meu Pluggy** (tier free, credenciais próprias). Não há aplicação
  Pluggy Connect multi-tenant; cada empresa = 1 Meu Pluggy. Custo **R$0 por
  empresa**. Em troca, o Conciliador passa a **guardar as credenciais do Meu
  Pluggy por empresa**, persistidas no banco e **criptografadas em repouso**
  (requisito firme — Backend §18.9). O widget Pluggy Connect continua, aberto com
  as credenciais da própria empresa. Resta confirmar apenas o rate limit do free
  tier.
- **Estimativa de volume (#13).** "~6 req/dia" é por empresa. Reescrever a seção
  de scheduling em termos de `nº empresas × contas`. A decisão de "uma instância,
  sem Quartz" (ARQUITETURA §11.6 / Backend §13) continua válida para o volume
  esperado, mas a justificativa textual precisa refletir o multiempresa.

---

## 5. O que já está consistente (não mexer)

- Coordenadas, stack e versões: Java 21, Spring Boot 4.1.x, PostgreSQL 16,
  Flyway dono do schema, `ddl-auto=validate`, Testcontainers — idênticos em
  ARQUITETURA §11, Backend §3 e README.
- Dinheiro como `BigDecimal`/`NUMERIC(14,2)`, `valor_liquido` como único valor de
  caixa, "casa antes de criar", transferência interna fora de receita/despesa —
  invariantes repetidas e coerentes em todos os documentos.
- Context path `/page`, isolamento por tenant via sessão, 404 cross-tenant, CSRF,
  outbox idempotente — coerentes entre Backend §10/§11 e as 9 telas.
- Base URL do Bling `https://api.bling.com.br/Api/v3` — igual em ARQUITETURA §7 e
  Backend §12.

---

## 6. Ação recomendada

1. Tratar ARQUITETURA-conciliador.md como **documento histórico** ou atualizá-lo
   para o modelo multiempresa (itens #1–#7). Recomendado atualizar, para não
   deixar um modelo de dados errado como primeira leitura de quem chega.
2. Aplicar as três padronizações de rota (#8–#10).
3. Acrescentar Bootstrap ao stack documentado no README/ARQUITETURA (#11).
4. Estimativa de volume (#13): reescrever em termos de `nº empresas × contas`.
   Custo Pluggy (#12) já está resolvido — cada empresa, seu próprio Meu Pluggy.

As correções #1–#7, #11 e #12 já foram aplicadas às specs. Resta apenas o
ajuste textual de estimativa (#13), que depende do número de empresas previsto.
