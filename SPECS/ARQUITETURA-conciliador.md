# Conciliador — Arquitetura

Serviço que coleta as movimentações financeiras das contas da empresa via Pluggy
(Open Finance), classifica cada transação e a concilia no Bling de forma híbrida
(API v3 no que é seguro, OFX para o restante).

> **Status:** arquitetura v1 (sincronizada). Itens marcados com ⚠️ precisam ser
> confirmados na referência da API antes da implementação.
>
> **Precedência:** o v1 é **multiempresa com form login e isolamento por tenant**
> (ver [Backend](./Backend.md) §1 e [Frontend](./Frontend.md)). As menções
> originais a empresa única e HTTP Basic foram substituídas. Divergências e
> correções aplicadas estão registradas em
> [AUDITORIA-consistencia.md](./AUDITORIA-consistencia.md). As incógnitas da API
> do Bling (§7) foram levantadas em [Bling-API-v3.md](./Bling-API-v3.md).

---

## 1. Objetivo

Automatizar a conciliação diária das contas (MP, Stone, Cora) sem replicar os
erros que inflaram o DRE de maio/2026: transferência interna contada como
receita/despesa, custo de fornecedor mal mapeado e valor de venda registrado em
duplicidade.

O serviço **não substitui** o motor de conciliação do Bling — ele alimenta e
pré-resolve a conciliação, deixando o trabalho manual só para o que for
genuinamente ambíguo.

---

## 2. Decisões de arquitetura (ADR resumido)

| # | Decisão | Motivo |
|---|---------|--------|
| 1 | Fonte única: **Pluggy via Open Finance** — **cada empresa usa o seu próprio Meu Pluggy** (tier free, credenciais próprias) | Uma integração cobre todas as contas da empresa; **custo R$0 por empresa** (free tier de cada uma); infra regulada pelo BC. As credenciais do Meu Pluggy são **por empresa, persistidas no banco e criptografadas em repouso** — nunca globais no `.env`. O widget Pluggy Connect é aberto com as credenciais da própria empresa. |
| 2 | **Serviço novo separado, com UI própria** | Isola do Gestor de Pedidos; a fila de revisão precisa de tela. |
| 3 | Escrita no Bling **híbrida**: API v3 no que tem certeza, OFX no resto | O Bling **não tem API de conciliação/import de OFX** — só tela. O que dá pra escrever com segurança vai por API; o resto cai no upload manual de OFX. |
| 4 | Classificação **automática + fila de revisão** para ambíguos | Regras resolvem o trivial; humano decide o duvidoso. |
| 5 | **Casa antes de criar** | O lado de *receber* já é populado pelo Nuvemshop/Gestor. Criar cegamente duplicaria. |
| 6 | **Transferência interna é classe de 1ª ordem** | É a principal causa de inflação do DRE. |
| 7 | **`valor_liquido` é o único valor de caixa** — nunca o valor de face | Registrar R$270 quando entrou R$255 infla o faturamento real. O bruto vive na conta a receber do Bling, não no caixa. |
| 8 | Taxa de adquirente **não é separada por adquirente** no v1 | Vai para um balde único "descontos/taxas financeiras", derivada do gap bruto − líquido. |

---

## 3. Componentes do serviço

```
┌─────────────────────────────────────────────────────────────┐
│                       Conciliador                            │
│                                                              │
│  Scheduler ──► Ingest Worker ──► Normalizer ──► Classifier   │
│  (1x/dia)      (Pluggy API)      (canônico)     (tipo+conf)  │
│                                       │                      │
│                                       ▼                      │
│                                   Matcher ◄──── Bling (GET)  │
│                                       │                      │
│                 ┌─────────────────────┼──────────────────┐  │
│                 ▼                     ▼                  ▼   │
│            Writer/Outbox        Review Queue       OFX Gen   │
│            (Bling POST)         (UI web)           (arquivo) │
└─────────────────────────────────────────────────────────────┘
```

- **Scheduler** — cron diário, percorrendo as integrações ativas **por empresa**
  (~6 req/dia por empresa, 1/conta). O volume total escala com `nº empresas ×
  contas`; ainda assim cabe em uma única instância sem Quartz (§11.6). Dispara o
  ingest.
- **Ingest Worker** — chama a API do Pluggy (contas, saldos, transações),
  paginando por janela de data. Idempotente pelo `pluggy_transaction_id`.
- **Normalizer** — converte cada transação no formato canônico (tabela §4).
  **Aqui se aplica a regra do valor líquido.**
- **Classifier** — atribui `classe` + `confianca` (§5).
- **Matcher** — consulta o Bling (contas a receber/pagar abertas) e busca
  correspondência. Também pareia as duas pernas de transferência interna.
- **Writer/Outbox** — escreve no Bling via API v3 com padrão outbox
  (idempotência + retry), só para itens de alta confiança.
- **Review Queue** — tela web; lista os ambíguos com sugestão e candidatos de
  match; o humano confirma/corrige → vira escrita API ou entra no lote OFX.
- **OFX Generator** — gera OFX por conta para o que não foi escrito por API;
  upload manual no Bling (Financeiro › Caixas e Bancos).

---

## 4. Modelo de dados — tabela `transacao`

Tabela central do pipeline e fonte de verdade do estado de cada movimento.

```sql
CREATE TABLE transacao (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Tenant (raiz de isolamento — obrigatório em toda tabela compartilhada)
  empresa_id              UUID NOT NULL REFERENCES empresa(id),

  -- Origem (idempotência de ingestão)
  pluggy_transaction_id   TEXT NOT NULL,          -- único por empresa (ver constraint abaixo)
  pluggy_account_id       TEXT NOT NULL,
  conta_local             TEXT NOT NULL,          -- 'mp' | 'stone' | 'cora' ...

  -- Dados do movimento
  data                    DATE NOT NULL,
  valor_liquido           NUMERIC(14,2) NOT NULL, -- SEMPRE o que pingou. Nunca o valor de face.
  direcao                 TEXT NOT NULL,          -- 'credito' | 'debito'
  descricao_raw           TEXT,
  contraparte_doc         TEXT,                   -- CPF/CNPJ da contraparte, se vier
  e2e_id                  TEXT,                   -- end-to-end id do PIX, se vier

  -- Classificação
  classe                  TEXT NOT NULL DEFAULT 'indefinido',
                          -- 'credito_venda' | 'transferencia_interna'
                          -- | 'debito_despesa' | 'pro_labore' | 'indefinido'
  confianca               NUMERIC(4,3) NOT NULL DEFAULT 0,  -- 0.000 .. 1.000

  -- Match contra o Bling
  match_bling_tipo        TEXT,                   -- 'conta_receber' | 'conta_pagar' | null
  match_bling_id          TEXT,                   -- id da conta no Bling
  taxa_derivada           NUMERIC(14,2),          -- bruto(Bling) − liquido, quando casou

  -- Pareamento de transferência
  transfer_par_id         UUID REFERENCES transacao(id),  -- a outra perna

  -- Estado e escrita
  estado                  TEXT NOT NULL DEFAULT 'ingerido',
                          -- ver §6
  bling_bordero_id        TEXT,                   -- id da baixa, quando escrita por API
  ofx_lote_id             TEXT,                   -- id do lote OFX, quando vai por OFX

  -- Resiliência: detalhes técnicos de entrega (tentativas, erro_ultimo) vivem em
  -- outbox_bling, não aqui. estado = FALHA indica falha terminal de negócio.
  -- Ver Backend §7.2.

  version                 BIGINT NOT NULL DEFAULT 0,  -- optimistic locking (@Version)
  created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_transacao_pluggy UNIQUE (empresa_id, pluggy_transaction_id)
);

CREATE INDEX idx_transacao_estado ON transacao(empresa_id, estado);
CREATE INDEX idx_transacao_match  ON transacao(empresa_id, data, valor_liquido, direcao);
```

### Invariante crítica

> `valor_liquido` **nunca** recebe o valor de face de uma venda. O valor bruto
> (ex.: R$270) é da `conta a receber` no Bling. O caixa enxerga só o líquido
> (ex.: R$255). A diferença é `taxa_derivada`. Um número nunca sobrescreve o outro.

---

## 5. Regras de classificação e match

A ordem importa: rodar **match de conta a receber primeiro** evita que uma venda
e um pagamento de mesmo valor no mesmo dia sejam confundidos com transferência.

### 5.1 Crédito de venda (casa com conta a receber)

A `conta a receber` já existe (criada pelo Nuvemshop/Gestor). O crédito que
pingou é **menor** que o bruto, pela taxa.

- **Regra de match:** existe conta a receber aberta tal que
  `bruto ≥ liquido` **e** `(bruto − liquido) / bruto ≤ 0.10` (tolerância de taxa,
  ajustável) **e** `data` dentro da janela.
- **Taxa derivada:** `taxa = bruto − liquido` → balde único de despesa financeira.
- **Baixa 1:N:** baixa a conta a receber de R$270 usando R$255 de crédito real
  **+ R$15 de taxa**. A conta fecha, o caixa reflete R$255, o faturamento conta
  R$270 uma única vez.
- **Crédito SEM conta a receber correspondente → ALERTA**, vai pra revisão.
  Não é rotina: é estorno, aporte, ou um pedido que o fluxo perdeu. Detector de
  furo de graça.

### 5.2 Transferência interna (pareia as duas pernas)

Uma transferência entre contas próprias deixa **duas pegadas**: débito na conta A
e crédito na conta B, ambas conectadas no Pluggy. Pagamento a terceiro deixa
**uma só**.

- **Match determinístico (alta confiança):** mesma transação dos dois lados pelo
  `e2e_id` (PIX carrega o mesmo E2E nas duas pernas), ou `contraparte_doc`
  batendo com uma das contas próprias.
- **Fallback (média confiança):** valor absoluto igual (com tolerância p/ tarifa
  de TED), direção oposta, contas diferentes, janela curta (PIX: segundos;
  TED: mesmo dia / próximo dia útil).
- **Booking:** registra como **movimentação entre contas — nunca receita/despesa.**
  As duas pernas linkadas via `transfer_par_id`.
- ⚠️ Ver §7: a v3 pode não ter endpoint de transferência; nesse caso esta classe
  roteia para OFX.

### 5.3 Débito = despesa (lado que mais cria)

O integrador de pedidos não lança contas a pagar. Fornecedor, despesa
operacional, tarifa: nada está no Bling ainda.

- Se casar com uma conta a pagar já criada → baixa.
- Se não houver → cria conta a pagar + baixa (alta confiança) ou manda pra
  revisão (baixa confiança). Aqui mora o mapeamento de categoria que quebrou em maio.

---

## 6. Máquina de estados (`estado`)

```
ingerido ──► classificado ──┬──► aguardando_escrita_api ──► escrito_api ──► conciliado
                            │
                            ├──► em_revisao ──┬──► aguardando_escrita_api ──► escrito_api ──► conciliado
                            │                 └──► em_lote_ofx ──► conciliado
                            │
                            └──► em_lote_ofx ──► conciliado

(escrita API) ──(tentativas esgotadas)──► falha ──(retry)──► aguardando_escrita_api
```

`aguardando_escrita_api` é o item já enfileirado no outbox, ainda não confirmado
pelo Bling — distinguir "enfileirado" de "confirmado" é exigência do padrão
outbox. O conjunto canônico de 8 estados e a tabela completa de transições estão
em [Backend](./Backend.md) §5.3. `conciliado` é terminal, não importa por qual
caminho chegou.

---

## 7. Mapa transação → API v3 do Bling

Base: `https://api.bling.com.br/Api/v3` · Auth: OAuth 2.0 Bearer.

| Necessidade | Operação | Endpoint | Status |
|---|---|---|---|
| Listar contas a receber abertas (p/ match) | GET | `/contas/receber` | ✅ confirmado |
| Listar contas a pagar abertas (p/ match) | GET | `/contas/pagar` | ✅ confirmado |
| Crédito casa c/ receber → baixa 1:N (crédito + taxa) | POST | `/borderos` | ✅ recurso existe; ⚠️ confirmar campos de taxa/desconto na baixa |
| Criar despesa nova (fornecedor) | POST | `/contas/pagar` | ✅ confirmado |
| Baixar a despesa criada | POST | `/borderos` | ✅ recurso existe |
| Categorias (balde de taxa, despesas) | GET | `/categorias/receitas-despesas` | ✅ confirmado |
| Contas/portadores bancários | GET | `/contas-contabeis` | ⚠️ confirmar se é aqui que vivem as contas bancárias |
| **Transferência interna entre contas** | — | **sem endpoint na v3 (só UI)** | ✅ **decisão fechada: sai por OFX** (ver [Bling-API-v3.md](./Bling-API-v3.md) §4) |

> **Observação importante (honesta):** a API v3 movimenta dinheiro principalmente
> via **borderô (baixa)** de contas a pagar/receber. Não há endpoint de
> "lançamento de caixa avulso" nem de "transferência entre contas" (confirmado —
> existe só na UI). Logo: crédito-de-venda e despesa-nova são expressáveis por
> API; **transferência interna e pró-labore saem por OFX** no v1. Os campos exatos
> do POST de borderô (taxa/desconto) e do portador ainda precisam de confirmação
> no sandbox — ver [Bling-API-v3.md](./Bling-API-v3.md) §3 e §7.

---

## 8. Idempotência e deduplicação

Duas camadas, ambas no seu repertório (outbox/CAPI):

1. **Ingestão:** `UNIQUE(empresa_id, pluggy_transaction_id)`. O mesmo movimento
   aparecendo em pulls de dias diferentes não duplica, e a deduplicação é
   escopada por empresa.
2. **Escrita no Bling:** padrão outbox. A transação só sai do estado `escrito_api`
   quando o `bling_bordero_id` volta. Em retry, checar se já existe baixa antes de
   reenviar (idempotência da escrita). Nunca criar lançamento sem passar pela
   regra "casa antes de criar" (§2.5) — é a salvaguarda contra duplicar o que o
   Nuvemshop/Gestor já jogam.

---

## 9. Fila de revisão e casos de borda

Vão para a fila (não automatizar no v1):

- **Parcelado:** uma venda em 3x cai como ~3 créditos líquidos ao longo de meses
  → match deixa de ser 1:1, vira "vários créditos amortizando uma conta a
  receber", com taxa em cada parcela.
- **Antecipação:** líquido cai antes e com taxa maior → o gap estoura a tolerância
  de ~10% e parece "não é venda". Marcar p/ cair na revisão, não virar
  falso-negativo.
- **Crédito sem conta a receber:** alerta de furo no fluxo de pedidos.
- **Transferência p/ conta não conectada / conta pessoal:** só uma perna → "saída
  sem par". Força classificar ou conectar a conta que falta.
- **Pró-labore / retirada de sócio:** **não é transferência interna** — é
  distribuição (patrimônio), não despesa. Classe à parte.

---

## 10. Riscos e pontos em aberto

- ⚠️ **Bling sem API de conciliação/OFX** — por isso o desenho é híbrido; a perna
  OFX continua sendo upload manual.
- ⚠️ **Transferência interna via API** — provável fallback p/ OFX (§7).
- ⚠️ **Consentimento Open Finance** — o Pluggy avisa e a renovação é simples, mas
  não é zero-toque; PJ com múltiplas alçadas tem fricção extra.
- **Custo do Pluggy: resolvido.** Cada empresa usa o **seu próprio Meu Pluggy**
  (tier free), então o custo é **R$0 por empresa**, sem licença multi-tenant. Em
  contrapartida, o Conciliador passa a **guardar as credenciais do Meu Pluggy por
  empresa**, o que torna a **criptografia em repouso** dessas credenciais um
  requisito firme (não mais opcional — ver Backend §18.9). Confirmar apenas o
  rate limit do free tier (volume por empresa ~6 req/dia é folgado).
- **Granularidade de adquirência** — v1 registra líquido e deriva a taxa do gap.
  Se um dia quiser taxa por adquirente, entra MP/Stone direto só p/ essas contas.

---

## 11. Setup e convenções

### 11.1 Bootstrap do projeto

- Gerar o esqueleto pelo **Spring Initializr** com **Spring Boot 4.1.x**, **Java
  21** e **Maven**.
- Incluir as dependências: Spring Web, Thymeleaf, Spring Data JPA, PostgreSQL
  Driver, Flyway Migration, Validation, Actuator e Spring Security.
- Não usar Lombok. Para DTOs, preferir `record` do Java 21.
- Configurar desde o início **form login com sessão e CSRF** (Spring Security),
  protegendo toda a aplicação exceto cadastro, login, estáticos, callbacks
  estritamente necessários e `/actuator/health`. O isolamento por tenant
  (`empresa_id` da sessão) acompanha a segurança desde o passo 1. Ver
  [Setup-SpringBoot.md](./Setup-SpringBoot.md) §7.

### 11.2 Organização por feature

Organizar o código por domínio, espelhando os componentes da arquitetura, e não
por camadas técnicas genéricas. Os pacotes de topo são:

```text
ingest
transacao
classificacao
match
bling
outbox
ofx
revisao
config
```

Evitar pacotes globais como `controller`, `service` e `repository`. Controllers,
serviços, repositórios e demais tipos ficam dentro do domínio ao qual pertencem.

### 11.3 Banco de dados e migrations

- O **Flyway é o único dono do schema**. A migration `V1__transacao.sql` deve
  criar a tabela descrita no §4; as entidades JPA apenas mapeiam esse schema.
- Configurar `spring.jpa.hibernate.ddl-auto=validate`. Nunca usar `update`.
- Valores monetários são representados por `BigDecimal` no Java e
  `NUMERIC(14,2)` no PostgreSQL. Nunca usar `double` para dinheiro.

### 11.4 Integrações HTTP

- Implementar os clientes do Pluggy e do Bling como interfaces declarativas com
  `@HttpExchange` sobre `RestClient`, cada uma em seu pacote de domínio.
- Converter as respostas externas para o modelo canônico no normalizer, na
  borda da aplicação.
- A API v3 do Bling usa OAuth 2.0 e o access token expira. Implementar desde o
  início um componente que armazene e renove o token, com persistência para
  sobreviver ao reinício do container.

### 11.5 Ingestão, outbox e idempotência

- O worker de ingestão grava somente `transacao`, com idempotência garantida por
  `UNIQUE(pluggy_transaction_id)`.
- A escrita no Bling ocorre em outro worker, que consome o outbox e verifica se
  a baixa já existe antes de reenviar.
- Nunca chamar a escrita do Bling dentro da transação de ingestão.

### 11.6 Agendamento

Usar `@EnableScheduling` e `@Scheduled(cron = ...)` para o pull diário. Com o
volume estimado de aproximadamente seis requisições por dia e uma única
instância da aplicação, não há necessidade de Quartz nem de coordenação de
cluster.

### 11.7 Interface web

- Usar Thymeleaf com HTMX e **Bootstrap** fornecidos por **WebJar**, com versão
  fixada e disponível offline; não carregar nada por CDN. Identidade visual por
  CSS próprio com custom properties. Ver [Frontend](./Frontend.md) §3 e
  [Frontend-Implementacao.md](./Frontend-Implementacao.md) §3.
- Implementar a fila de revisão como uma página Thymeleaf e um `th:fragment`.
  Ações de aprovar, editar e rotear retornam o fragmento HTML que o HTMX
  substitui na tela.
- Não criar uma API JSON para a interface web.

### 11.8 Imagem e execução local

- Gerar a imagem da aplicação com `./mvnw spring-boot:build-image`, usando Cloud
  Native Buildpacks. Não manter um Dockerfile próprio.
- O Compose deve ter dois serviços: a aplicação e `postgres:16`.
- Usar volume nomeado para o banco, healthcheck da aplicação pelo Actuator e
  `depends_on` condicionado à saúde do PostgreSQL.

### 11.9 Configuração e segredos

- Manter em `application.yml` somente placeholders como `${BLING_CLIENT_SECRET}` e
  `${CRIPTO_KEY}` (chave de cifragem). As credenciais do **Meu Pluggy não são
  globais**: cada empresa informa as suas, que ficam no banco criptografadas.
- Fornecer os valores de ambiente por variáveis. No desenvolvimento, usar um
  arquivo `.env` incluído no `.gitignore`.
- Nunca versionar credenciais.

### 11.10 Testes de integração

Usar Testcontainers com `@ServiceConnection` para subir PostgreSQL real nos
testes. Não usar H2: diferenças na semântica de SQL e de valores monetários
atingem diretamente as invariantes centrais deste serviço.
