# Conciliador — API v3 do Bling: o que está definido

> **Status:** levantamento v1 para fechar os ⚠️ de ARQUITETURA §7.
> **Base:** `https://api.bling.com.br/Api/v3` · OAuth 2.0 Bearer.
> **Fontes:** referência oficial (developer.bling.com.br/referencia — SPA, não
> indexável por fetch), SDK open-source `bling-erp-api`, wiki Kondado e central
> de ajuda do Bling. Onde o schema exato do POST não pôde ser extraído da SPA,
> o item fica marcado **⚠️ confirmar no Postman/sandbox**.

---

## 1. Conclusão executiva

Três das quatro incógnitas de ARQUITETURA §7 ficam **resolvidas em direção**:

1. **Leitura (match)** — `GET` de contas a pagar e a receber: **confirmado**.
2. **Baixa (escrita)** — feita por **borderô**: recurso confirmado. O **split
   líquido + taxa** existe no modelo do Bling via **forma de pagamento** (que tem
   `taxas` e `destino`) e via os campos de desconto/acréscimo do borderô — porém
   o **schema exato do POST de borderô precisa ser confirmado no sandbox**.
3. **Transferência interna** — **não há endpoint v3 dedicado** (existe só na UI,
   Financeiro › Caixas e Bancos). **Decisão fechada: transferência interna sai
   por OFX no v1.**
4. **Onde vivem as contas bancárias (portadores)** — o campo `portador` aparece
   em contas a pagar/receber e o `destino` da forma de pagamento aponta para
   "caixa e bancos"; **não há evidência de um GET público que liste portadores**.
   Tratar o id do portador como **configuração por empresa** (mapeado uma vez),
   não como recurso descoberto via API. **⚠️ confirmar no sandbox.**

---

## 2. Mapa de recursos confirmados

| Necessidade no Conciliador | Método | Recurso v3 | Situação |
|---|---|---|---|
| Listar contas a receber abertas (match de venda) | GET | `contas a receber` (`contasreceber`) | ✅ recurso existe |
| Listar contas a pagar abertas (match de despesa) | GET | `contas a pagar` (`contaspagar`) | ✅ recurso existe |
| Criar despesa nova (fornecedor) | POST | `contas a pagar` | ✅ recurso existe |
| Baixar conta (receber/pagar) | POST | `borderôs` | ✅ recurso existe; ⚠️ campos de taxa/desconto a confirmar |
| Categorias (balde de taxa, despesas) | GET | `categorias de receitas e despesas` | ✅ recurso existe |
| Formas de pagamento (taxa + destino) | GET | `formas de pagamento` | ✅ recurso existe; carrega `taxas` e `destino` |
| Plano de contas | GET | `contas contábeis` | ✅ recurso existe (tem alias de integração) |
| Portador / conta financeira (id de caixa-banco) | — | sem GET público evidente | ⚠️ tratar como config por empresa |
| Transferência interna entre contas | — | **sem endpoint v3** | ❌ → roteia para OFX |

Campos observados nas contas (relevantes ao match e à baixa):

- **Contas a pagar:** contato, forma de pagamento, situação (`aberto`,
  `recebido`, `parcialmente recebido`, `devolvido`, `cancelado`), valor,
  vencimento, categoria, **portador**, número do documento, saldo atual,
  vencimento original, e subtabelas de **borderôs** e **pagamentos de borderô**.
- **Contas a receber:** estrutura análoga + **conta contábil**, origem (documento
  gerador) e tipo de origem, **id de transação**, vendedor.
- **Forma de pagamento:** `destino` (conta a receber/pagar, ficha financeira,
  **caixa e bancos**) e `taxas` (**alíquota, prazo, valor em R$**).

---

## 3. Como modelar o split líquido + taxa (decisão de desenho)

O princípio do produto (README / ARQUITETURA §2.7) é: a venda bruta (ex. R$270)
vive na **conta a receber**; no caixa entra só o **líquido** (R$255); a diferença
(R$15) é **taxa financeira**. No Bling, há dois caminhos para expressar isso na
baixa:

- **Caminho A — desconto/taxa no próprio borderô.** A baixa quita a conta a
  receber de R$270 registrando R$255 de crédito real + R$15 de taxa/desconto no
  borderô. Requer confirmar quais campos o POST de borderô expõe para isso.
  **(preferido, se os campos existirem)**
- **Caminho B — taxa pela forma de pagamento.** Cadastrar a forma de pagamento
  com `taxas` e `destino = caixa e bancos`; o Bling deriva a taxa na liquidação.
  Mais próximo do fluxo nativo, porém menos determinístico para conferência.

**Recomendação:** mirar o **Caminho A** (controle explícito do valor por baixa,
coerente com `taxa_derivada` da `transacao`), e confirmar no sandbox o nome dos
campos antes de fechar `BlingDtos`. Manter o Caminho B como plano B.

> Em qualquer caminho, a invariante se mantém: `valor_liquido` da `transacao`
> nunca recebe o bruto; o bruto é só a referência da conta a receber no Bling.

---

## 4. Transferência interna — decisão fechada

A API v3 **não oferece** endpoint de "transferência entre contas" nem de
"lançamento de caixa avulso". A funcionalidade existe apenas na interface do Bling
(Financeiro › Caixas e Bancos › Transferência entre contas).

**Consequência:** a classe `TRANSFERENCIA_INTERNA` (e `PRO_LABORE`, que também não
é despesa) **não é escrita por API no v1** — é roteada para **OFX**. Isso fecha o
⚠️ de ARQUITETURA §5.2 e §7 e confirma o desenho híbrido: API v3 para
crédito-de-venda e despesa-nova; OFX para transferência interna, pró-labore e o
que sobrar.

---

## 5. Idempotência da escrita no Bling

ARQUITETURA §8 e Backend §6.5 exigem "checar se a baixa já existe antes de
reenviar". Como confirmar a existência de uma baixa:

- A conta a pagar/receber tem **situação** e subtabela de **borderôs/pagamentos**.
  Antes de postar um novo borderô, o worker do outbox faz `GET` da conta e
  verifica se já está `recebido`/`parcialmente recebido` com o borderô esperado.
- Usar a **chave de idempotência** do outbox (`outbox_bling.chave_idempotencia`,
  Backend §7.2) como referência local; o lado Bling é verificado por leitura da
  conta, não por um header de idempotência (a v3 não documenta um).
- Onde houver **alias de integração** (contas contábeis / contatos), usá-lo para
  correlacionar registros criados pelo Conciliador. **⚠️ confirmar** se contas a
  pagar criadas via API aceitam um campo de referência externa estável.

---

## 6. OAuth 2.0 (já alinhado com Backend §9.2)

- Fluxo *authorization code*; access token + refresh token com expiração.
- Persistir `access_token`, `refresh_token`, `expires_at` por empresa
  (`bling_oauth_token`, migration `V5`).
- Renovar com margem configurável e **serializar o refresh** (lock pessimista na
  linha). Nunca logar token.
- Token sobrevive a restart (persistido). Refresh revogado → parar escritas e
  expor falha no Actuator.

Nada aqui contradiz as specs; é a confirmação de que o modelo de token desenhado
no Backend é compatível com o OAuth real do Bling.

---

## 7. Itens a confirmar no sandbox antes de fechar `BlingDtos`

1. Schema do **POST de borderô**: campos para valor pago, desconto/taxa,
   portador/conta de destino e vínculo com a conta quitada.
2. Existência (ou não) de **GET de portadores/contas financeiras**; se não houver,
   formalizar o portador como configuração por empresa.
3. Campo de **referência externa** em conta a pagar criada por API (para
   idempotência forte além da leitura de situação).
4. Filtros de **GET de contas a receber/pagar** por situação=aberto e janela de
   datas (paginação), para o Matcher.
5. Limites de **rate limit** da v3 (a ARQUITETURA assume folga; confirmar teto).

Esses cinco pontos são o conteúdo da "homologação" do Bling e devem virar testes
de client com stub HTTP (Backend §15.4) espelhando as respostas reais.

---

## 8. Reflexo nas specs

- ARQUITETURA §7: trocar o ⚠️ de transferência interna por **decisão fechada
  (OFX)**; manter ⚠️ apenas nos campos de borderô e portador, agora com o caminho
  A/B definido aqui.
- Backend §9.1 (`BlingGateway`): a assinatura proposta
  (`buscarContasReceber/Pagar`, `buscarBaixa`, `criarContaPagar`, `criarBordero`)
  **continua válida** — todos os recursos por trás dela existem.
- Backend §18 (decisões pendentes 4, 5, 6): a 6 (transferência interna) fica
  **resolvida**; as 4 e 5 (baixa existente / payload de borderô) viram os itens
  §7.1–§7.3 acima.

> **Fontes:** [Referência da API Bling](https://developer.bling.com.br/referencia)
> · [Autenticação Bling API](https://developer.bling.com.br/bling-api)
> · [SDK bling-erp-api (entidades v3)](https://github.com/AlexandreBellas/bling-erp-api-js)
> · [Wiki Bling v3 — Kondado](https://kondado.com.br/wiki/a/bling-v3)
> · [Transferir valores entre contas (UI)](https://ajuda.bling.com.br/hc/pt-br/articles/360038793993-Transferir-valores-entre-contas)
> · [Controlar taxas por forma de pagamento](https://ajuda.bling.com.br/hc/pt-br/articles/360035872974-Como-controlar-as-taxas-que-pago-sobre-algumas-formas-de-pagamento-no-Bling)
