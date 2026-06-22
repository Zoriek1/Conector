# Conciliador — Especificação do backend

> **Status:** proposta v0.2  
> **Documento-base:** [ARQUITETURA-conciliador.md](./ARQUITETURA-conciliador.md)  
> **Escopo:** backend Spring Boot, persistência, integrações, segurança e contratos
> usados pela interface Thymeleaf/HTMX.

> **Precedência:** este documento substitui as decisões anteriores de empresa
> única e HTTP Basic do documento geral. O requisito atual é multiempresa com
> form login e isolamento por tenant.

---

## 1. Objetivo

Detalhar a implementação do backend do Conciliador sem transformar o sistema em
uma coleção de scripts ou serviços anêmicos. O desenho usa orientação a objetos
para concentrar regras e invariantes nos objetos de domínio e mantém integrações
externas nas bordas.

O v1 é multiempresa: cada cadastro cria uma empresa e um único acesso, usado pelo
proprietário ou contador. Dados financeiros, integrações e arquivos pertencem à
empresa e nunca ao login diretamente. Uma única instância atende todas as
empresas; execução em cluster e API pública não fazem parte do escopo.

---

## 2. Princípios de implementação

1. **Modelo de domínio rico:** `Transacao` controla suas mudanças de estado. Não
   existem setters públicos nem alteração direta de campos por controllers ou
   workers.
2. **Composição antes de herança:** regras variáveis implementam interfaces e são
   compostas em pipelines. Não criar uma árvore de subclasses de transação.
3. **Uma responsabilidade por classe:** scheduler dispara caso de uso; caso de
   uso coordena; entidade protege invariantes; client HTTP apenas traduz HTTP.
4. **Dependências apontam para dentro:** domínio não conhece DTOs do Pluggy,
   payloads do Bling, Thymeleaf ou `RestClient`.
5. **DTOs imutáveis:** requests, responses, comandos e projeções usam `record`.
6. **Sem abstrações preventivas:** não criar framework interno, `BaseService`,
   `BaseRepository`, `AbstractEntity` ou interfaces sem uma fronteira real.
7. **Transações curtas:** nenhuma chamada HTTP ocorre dentro de transação de
   banco mantida aberta.
8. **Tenant obrigatório:** toda operação de negócio é executada no contexto da
   empresa obtida da sessão autenticada, nunca de um parâmetro enviado pelo
   navegador.

---

## 3. Stack e coordenadas

- Java 21.
- Spring Boot 4.1.x.
- Maven Wrapper.
- Spring Web MVC, Thymeleaf e HTMX via WebJar.
- Spring Data JPA e PostgreSQL 16.
- Flyway como único mecanismo de criação e evolução do schema.
- Bean Validation.
- Spring Security com form login, sessão e CSRF habilitado.
- Actuator.
- Testcontainers com `@ServiceConnection`.

Coordenadas definidas:

```text
groupId:      com.planteumaflor
artifactId:   conciliador
package-base: com.planteumaflor.conciliador
```

URL pública prevista:

```text
https://conciliador.planteumaflor.com/page
```

O domínio DNS é escrito em minúsculas. `/page` é o context path HTTP e não faz
parte do pacote Java. Nos exemplos deste documento, `<base>` representa
`com.planteumaflor.conciliador`.

---

## 4. Estrutura de pacotes

O primeiro nível é organizado por feature. Dentro de cada feature, subpacotes
separam as responsabilidades que realmente existem. Não criar todas as
subcamadas por obrigação: `inicio` precisa apenas de `web` e `query`, enquanto
`transacao` possui domínio, aplicação, persistência, query e web.

```text
<base>
├── ConciliadorApplication.java
├── identidade
│   ├── domain
│   │   ├── Usuario.java
│   │   ├── UsuarioId.java
│   │   └── UsuarioRepository.java
│   ├── application
│   │   ├── CadastrarEmpresaEUsuario.java
│   │   ├── CadastroService.java
│   │   └── AlterarSenha.java
│   ├── web
│   │   ├── LoginController.java
│   │   ├── CadastroController.java
│   │   ├── PerfilController.java
│   │   └── CadastroForm.java
│   ├── persistence
│   │   ├── UsuarioJpaRepository.java
│   │   └── UsuarioRepositoryAdapter.java
│   └── security
│       ├── UsuarioPrincipal.java
│       └── UsuarioDetailsService.java
├── empresa
│   ├── domain
│   │   ├── Empresa.java
│   │   ├── EmpresaId.java
│   │   └── EmpresaRepository.java
│   └── persistence
│       ├── EmpresaJpaRepository.java
│       └── EmpresaRepositoryAdapter.java
├── pluggy
│   ├── domain
│   │   ├── IntegracaoPluggy.java
│   │   ├── ContaBancaria.java
│   │   └── IntegracaoPluggyRepository.java
│   ├── application
│   │   ├── GerenciarPluggy.java
│   │   └── PluggyConnectService.java
│   ├── client
│   │   ├── PluggyClient.java
│   │   ├── PluggyClientConfig.java
│   │   └── PluggyDtos.java
│   ├── persistence
│   │   └── IntegracaoPluggyRepositoryAdapter.java
│   └── web
│       ├── PluggyController.java
│       └── PluggyWebhookController.java
├── onboarding
│   ├── application
│   │   ├── ConsultarOnboarding.java
│   │   └── IniciarPrimeiraSincronizacao.java
│   └── web
│       └── OnboardingController.java
├── inicio
│   ├── query
│   │   ├── ConsultarInicio.java
│   │   └── InicioQueryService.java
│   └── web
│       └── InicioController.java
├── integracoes
│   ├── query
│   │   ├── ConsultarIntegracoes.java
│   │   └── IntegracoesQueryService.java
│   └── web
│       └── IntegracoesController.java
├── ingest
│   ├── application
│   │   ├── IngestirTransacoes.java
│   │   └── PluggyNormalizer.java
│   └── scheduling
│       └── IngestScheduler.java
├── transacao
│   ├── domain
│   │   ├── Transacao.java
│   │   ├── EstadoTransacao.java
│   │   ├── ClasseTransacao.java
│   │   ├── Direcao.java
│   │   ├── Confianca.java
│   │   └── TransacaoRepository.java
│   ├── query
│   │   ├── ConsultarTransacoes.java
│   │   └── TransacaoQueryService.java
│   ├── persistence
│   │   ├── TransacaoJpaRepository.java
│   │   └── TransacaoRepositoryAdapter.java
│   └── web
│       └── TransacaoController.java
├── classificacao
│   ├── domain
│   │   ├── RegraClassificacao.java
│   │   └── ResultadoClassificacao.java
│   └── application
│       ├── ClassificarTransacoes.java
│       ├── Classificador.java
│       └── regras
├── match
│   ├── domain
│   │   ├── EstrategiaMatch.java
│   │   └── MatchCandidato.java
│   └── application
│       ├── EncontrarMatches.java
│       ├── Matcher.java
│       └── estrategias
├── bling
│   ├── domain
│   │   ├── BlingToken.java
│   │   └── BlingTokenRepository.java
│   ├── application
│   │   ├── BlingGateway.java
│   │   └── BlingTokenService.java
│   ├── client
│   │   ├── BlingClient.java
│   │   ├── BlingClientConfig.java
│   │   └── BlingDtos.java
│   ├── persistence
│   │   └── BlingTokenRepositoryAdapter.java
│   └── web
│       └── BlingOAuthController.java
├── outbox
│   ├── domain
│   │   ├── EventoOutbox.java
│   │   └── EventoOutboxRepository.java
│   ├── application
│   │   ├── EnfileirarEscritaBling.java
│   │   └── ProcessarOutboxBling.java
│   ├── persistence
│   │   └── EventoOutboxRepositoryAdapter.java
│   └── scheduling
│       └── OutboxScheduler.java
├── ofx
│   ├── domain
│   │   ├── LoteOfx.java
│   │   ├── LoteOfxRepository.java
│   │   └── GeradorOfx.java
│   ├── application
│   │   ├── GerarLoteOfx.java
│   │   ├── GerenciarLotesOfx.java
│   │   └── ObterArquivoOfx.java
│   ├── query
│   │   └── ConsultarLotesOfx.java
│   ├── persistence
│   │   └── LoteOfxRepositoryAdapter.java
│   └── web
│       └── LoteOfxController.java
├── revisao
│   ├── application
│   │   └── RevisarTransacao.java
│   ├── query
│   │   ├── ConsultarFilaRevisao.java
│   │   └── RevisaoQueryService.java
│   └── web
│       ├── RevisaoController.java
│       └── RevisaoViewModels.java
└── config
    ├── SecurityConfig.java
    ├── SchedulingConfig.java
    ├── ClockConfig.java
    └── ConciliadorProperties.java
```

Responsabilidades dos subpacotes:

| Subpacote | Conteúdo permitido |
|---|---|
| `domain` | entidades, value objects, regras e portas de repositório |
| `application` | casos de uso, commands e implementações orquestradoras |
| `web` | controllers, forms e view models Thymeleaf/HTMX |
| `persistence` | Spring Data, adapters de repositório e mapeamento persistente |
| `client` | clients HTTP, configuração e DTOs externos |
| `query` | consultas otimizadas e projeções imutáveis de leitura |
| `scheduling` | gatilhos agendados; nenhuma regra de negócio |
| `security` | integração da identidade com Spring Security |

Não criar pacotes globais `controller`, `service`, `repository`, `entity` ou
`dto`. Também não criar `BaseController`, `BaseService` ou `BaseRepository`.

Somente contratos usados entre subpacotes devem ser públicos. Implementações
permanecem com a menor visibilidade aceita pelo Spring/JPA. A camada `web` depende
de `application`/`query`; estas não dependem de `web`.

Direção de dependências:

```text
web ─────────► application ─────────► domain
 │                    ▲                  ▲
 └──────────► query   │                  │
                      │                  │
client ── implementa porta     persistence ── implementa repository
scheduling ─────────► application
```

`application` conhece interfaces de gateway, não implementações em `client`.
`domain` não conhece controller, form, view model, `RestClient`, Spring Data ou
template. No v1, entidades de domínio podem carregar apenas anotações JPA de
mapeamento para evitar duplicação de modelos; acesso ao banco continua
encapsulado em `persistence`.

---

## 5. Modelo orientado a objetos

### 5.0 Tenant, usuário e empresa

`Empresa` é a raiz de isolamento dos dados. `Usuario` representa autenticação e
referencia exatamente uma empresa no v1. Não criar tabelas físicas por usuário;
as tabelas compartilhadas possuem `empresa_id`, chaves estrangeiras e índices
compostos.

```text
Empresa 1 ── 1 Usuario
Empresa 1 ── N IntegracaoPluggy
Empresa 1 ── N ContaBancaria
Empresa 1 ── N Transacao
Empresa 1 ── N EventoOutbox
Empresa 1 ── N LoteOfx
```

Separar `Empresa` de `Usuario` permite trocar o acesso do proprietário para o
contador sem transferir registros financeiros. O modelo poderá evoluir para
vários usuários por empresa sem alterar a propriedade dos dados.

Invariantes de identidade e tenancy:

- e-mail é normalizado e único entre usuários;
- senha é persistida somente como hash;
- `usuario.empresa_id` é obrigatório e único no v1;
- `integracao_pluggy` é única por `(empresa_id, pluggy_item_id)`;
- `conta_bancaria` é única por `(empresa_id, pluggy_account_id)`;
- `transacao` é única por `(empresa_id, pluggy_transaction_id)`;
- token Bling é único por empresa;
- registros financeiros não mudam de empresa;
- desativar usuário ou integração não apaga histórico.

`UsuarioPrincipal` expõe `usuarioId` e `empresaId` imutáveis durante a sessão. Um
caso de uso recebe o tenant por um componente de contexto autenticado, e não por
campos de comando controlados pelo navegador.

### 5.1 Agregado `Transacao`

`Transacao` é a raiz do agregado financeiro. A entidade JPA e o objeto de domínio
são o mesmo tipo no v1; duplicar modelos agora adicionaria mapeamento sem trazer
isolamento útil.

Responsabilidades:

- preservar `valorLiquido` como o valor efetivamente movimentado;
- impedir transições de estado inválidas;
- registrar classificação e confiança juntas;
- registrar ou remover uma sugestão de match de forma consistente;
- parear transferências nos dois lados;
- decidir se pode ir para revisão, API ou OFX;
- impedir alteração após `CONCILIADO`.

Formato conceitual:

```java
@Entity
@Table(name = "transacao")
public class Transacao {
    @Id
    private UUID id;

    private UUID empresaId;
    private String pluggyTransactionId;
    private String pluggyAccountId;
    private String contaLocal;
    private LocalDate data;
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    private Direcao direcao;

    @Enumerated(EnumType.STRING)
    private ClasseTransacao classe;

    @Enumerated(EnumType.STRING)
    private EstadoTransacao estado;

    @Version
    private long version;

    protected Transacao() {}

    public static Transacao ingerida(/* dados normalizados */) { /* ... */ }
    public void classificar(ResultadoClassificacao resultado) { /* ... */ }
    public void sugerirMatch(MatchCandidato candidato) { /* ... */ }
    public void enviarParaRevisao(String motivo) { /* ... */ }
    public void aprovarParaApi() { /* ... */ }
    public void rotearParaOfx() { /* ... */ }
    public void registrarEscrita(String blingBorderoId) { /* ... */ }
    public void conciliar() { /* ... */ }
}
```

O trecho é um contrato de forma, não uma implementação pronta. Métodos recebem
objetos de domínio ou comandos, nunca DTOs HTTP externos.

### 5.2 Objetos de valor e enums

| Tipo | Forma | Regra |
|---|---|---|
| `Confianca` | value object imutável | `0.000` a `1.000`, escala 3 |
| `Direcao` | enum | `CREDITO`, `DEBITO` |
| `ClasseTransacao` | enum | `CREDITO_VENDA`, `TRANSFERENCIA_INTERNA`, `DEBITO_DESPESA`, `PRO_LABORE`, `INDEFINIDO` |
| `EstadoTransacao` | enum | estados definidos na §5.3 |
| `MatchCandidato` | record | tipo, id externo, valor bruto, taxa, confiança e justificativa |
| `ResultadoClassificacao` | record | classe, confiança e justificativa |

`BigDecimal` monetário deve ser normalizado com escala 2 e
`RoundingMode.HALF_EVEN` na borda. Comparações usam `compareTo`, nunca `equals`.
Valores negativos não representam direção: `valorLiquido` é positivo e
`Direcao` carrega o sinal semântico.

### 5.3 Estados e transições

Estados persistidos:

```text
INGERIDO
CLASSIFICADO
EM_REVISAO
AGUARDANDO_ESCRITA_API
ESCRITO_API
EM_LOTE_OFX
CONCILIADO
FALHA
```

Transições permitidas:

| Origem | Destino | Operação |
|---|---|---|
| `INGERIDO` | `CLASSIFICADO` | classificação conclusiva |
| `INGERIDO` | `EM_REVISAO` | dado insuficiente ou inválido |
| `CLASSIFICADO` | `AGUARDANDO_ESCRITA_API` | rota API aprovada |
| `CLASSIFICADO` | `EM_REVISAO` | confiança abaixo do limite |
| `CLASSIFICADO` | `EM_LOTE_OFX` | rota OFX segura |
| `EM_REVISAO` | `AGUARDANDO_ESCRITA_API` | aprovação humana |
| `EM_REVISAO` | `EM_LOTE_OFX` | roteamento humano |
| `AGUARDANDO_ESCRITA_API` | `ESCRITO_API` | Bling confirmou a baixa |
| `AGUARDANDO_ESCRITA_API` | `FALHA` | tentativas esgotadas |
| `FALHA` | `AGUARDANDO_ESCRITA_API` | retry manual |
| `ESCRITO_API` | `CONCILIADO` | confirmação final |
| `EM_LOTE_OFX` | `CONCILIADO` | usuário confirma upload |

Qualquer outra transição lança exceção de domínio. `CONCILIADO` é terminal.

### 5.4 Polimorfismo nas regras

Classificação e match variam; portanto são pontos reais de polimorfismo:

```java
public interface RegraClassificacao {
    Optional<ResultadoClassificacao> avaliar(Transacao transacao);
    int prioridade();
}

public interface EstrategiaMatch {
    List<MatchCandidato> buscar(Transacao transacao);
    boolean suporta(Transacao transacao);
    int prioridade();
}
```

O Spring injeta listas ordenadas. O orquestrador executa as regras em ordem e
mantém a justificativa da decisão. Adicionar uma regra não exige alterar um
`switch` central.

Ordem inicial de match:

1. conta a receber aberta;
2. transferência por `e2eId`;
3. transferência por documento de conta própria;
4. transferência por valor, direção e janela;
5. conta a pagar aberta;
6. sugestão de nova despesa.

---

## 6. Casos de uso

### 6.1 Ingerir transações

`IngestScheduler` chama `IngestirTransacoes` uma vez ao dia usando a zona
`America/Sao_Paulo`. Cada execução percorre integrações Pluggy ativas e mantém o
`empresa_id` durante todo o fluxo.

Fluxo:

1. obter as integrações e contas bancárias ativas, agrupadas por empresa;
2. consultar o Pluggy fora de transação de banco;
3. paginar toda a janela solicitada;
4. normalizar cada item para um comando canônico;
5. abrir uma transação curta por página ou lote;
6. inserir ignorando IDs Pluggy já existentes para a mesma empresa;
7. encerrar sem classificar nem chamar o Bling.

Idempotência definitiva é a constraint
`UNIQUE (empresa_id, pluggy_transaction_id)`. Uma consulta prévia pode economizar
trabalho, mas nunca substitui a constraint.

### 6.2 Classificar

`ClassificarTransacoes` busca itens `INGERIDO`, executa o `Classificador` e pede
ao agregado que registre o resultado. Ausência de regra conclusiva envia o item
à revisão com motivo explícito.

Limites de confiança devem vir de propriedades tipadas, não de constantes
espalhadas:

```yaml
conciliador:
  classificacao:
    confianca-automatica: 0.900
    tolerancia-taxa: 0.100
```

### 6.3 Encontrar match

`EncontrarMatches` carrega os dados necessários do Bling antes de iniciar a
transação de escrita local. O resultado pode ser:

- um candidato inequívoco: registrar e rotear automaticamente;
- vários candidatos próximos: registrar sugestões e enviar para revisão;
- nenhum candidato: enviar para revisão ou sugerir nova despesa conforme classe;
- transferência interna determinística: parear as duas transações atomicamente.

As duas pernas de transferência são bloqueadas e alteradas na mesma transação.

### 6.4 Enfileirar escrita no Bling

`EnfileirarEscritaBling` executa em uma única transação de banco:

1. valida que `Transacao` pode seguir pela API;
2. muda o estado para `AGUARDANDO_ESCRITA_API`;
3. cria um `EventoOutbox` pendente para a mesma empresa, com chave idempotente
   única dentro do tenant.

Nenhuma chamada ao Bling é feita nesse caso de uso.

### 6.5 Processar outbox

`ProcessarOutboxBling`:

1. reivindica um lote de eventos vencidos para processamento;
2. fecha a transação de reivindicação;
3. consulta o Bling para verificar se a baixa já existe;
4. cria a conta a pagar, quando aplicável e ainda inexistente;
5. posta o borderô somente se necessário;
6. em nova transação local, conclui o evento e registra o ID no agregado;
7. em erro, agenda nova tentativa com backoff e preserva a mensagem sanitizada.

Não aplicar retry HTTP cego em `POST`. A checagem de existência precede cada
reenvio.

### 6.6 Revisar manualmente

`RevisarTransacao` expõe operações de aplicação específicas:

- `aprovarSugestao`;
- `alterarClassificacao`;
- `selecionarMatch`;
- `rotearParaOfx`;
- `solicitarRetry`;
- `confirmarConciliacaoOfx`.

Cada comando revalida o estado atual. O ID e a `version` enviados pelo formulário
protegem contra edição concorrente; conflito retorna HTTP 409 e recarrega o
fragmento atualizado.

### 6.7 Gerar OFX

`GerarLoteOfx` agrupa somente itens `EM_LOTE_OFX` da mesma conta. O lote guarda
os IDs incluídos e um checksum do arquivo. Download não muda o estado para
`CONCILIADO`; isso só ocorre após confirmação humana do upload no Bling.

---

## 7. Persistência e Flyway

### 7.1 Migrations iniciais

```text
V1__empresa_e_usuario.sql
V2__integracao_pluggy_e_conta_bancaria.sql
V3__transacao.sql
V4__outbox_bling.sql
V5__bling_oauth_token.sql
V6__lote_ofx.sql
V7__auditoria.sql
V8__indices_workers.sql
```

`ddl-auto=validate` em todos os ambientes. Testes também executam Flyway.

### 7.2 Refinamento do modelo geral

O schema do documento geral coloca `tentativas` e `erro_ultimo` em `transacao`.
No backend, esses campos pertencem a `outbox_bling`, pois descrevem entrega
técnica e não o movimento financeiro. `transacao.estado = FALHA` indica falha
terminal visível ao negócio; detalhes de retry ficam no evento.

Campos mínimos de `outbox_bling`:

```sql
id                UUID PRIMARY KEY
empresa_id        UUID NOT NULL REFERENCES empresa(id)
transacao_id      UUID NOT NULL REFERENCES transacao(id)
tipo              TEXT NOT NULL
chave_idempotencia TEXT NOT NULL
payload           JSONB NOT NULL
status            TEXT NOT NULL
tentativas        INT NOT NULL DEFAULT 0
proxima_tentativa TIMESTAMPTZ NOT NULL
erro_ultimo       TEXT
locked_at         TIMESTAMPTZ
created_at        TIMESTAMPTZ NOT NULL
updated_at        TIMESTAMPTZ NOT NULL
UNIQUE (empresa_id, chave_idempotencia)
```

O payload é um snapshot versionado do comando a enviar. Isso impede que uma
edição posterior da transação altere silenciosamente um evento já criado.

### 7.3 Concorrência

- `Transacao`, `EventoOutbox` e `BlingToken` usam `@Version`.
- Workers reivindicam lotes com `FOR UPDATE SKIP LOCKED`.
- Índices parciais devem cobrir os estados pendentes dos workers.
- FKs compostas ou validações equivalentes impedem associação entre registros de
  empresas diferentes.
- Timestamps persistidos usam UTC (`Instant`); datas bancárias usam `LocalDate`.
- O `Clock` é injetado para tornar expiração, janelas e backoff testáveis.

---

## 8. Integração Pluggy

`PluggyClient` é uma interface `@HttpExchange`. DTOs refletem o contrato externo
e não escapam dos pacotes `pluggy` e `ingest`.

As credenciais técnicas da aplicação Pluggy são globais e vêm do ambiente. A
conexão é iniciada depois do cadastro: a aplicação cria uma sessão/token de
conexão, o usuário conclui o fluxo do Pluggy e o backend persiste os
identificadores da integração e das contas com `empresa_id`. Nenhum identificador
de conexão de cliente fica no `.env`.

O frontend pode apresentar todas as contas em uma visão consolidada. O filtro de
conta bancária não altera o isolamento por empresa.

`PluggyNormalizer` deve:

- validar identificadores obrigatórios;
- converter data e direção;
- usar o valor líquido efetivamente informado pelo banco;
- normalizar documento sem pontuação;
- preservar descrição original;
- rejeitar moeda diferente de BRL no v1;
- produzir um comando canônico imutável.

Erros 401/403 interrompem o ciclo e geram alerta. Erros 429/5xx respeitam
`Retry-After` quando presente e não descartam páginas já persistidas.

---

## 9. Integração Bling e OAuth 2.0

### 9.1 Clientes

Separar leitura e escrita na API da aplicação, ainda que usem o mesmo client
HTTP:

```java
public interface BlingGateway {
    List<ContaReceber> buscarContasReceber(/* janela */);
    List<ContaPagar> buscarContasPagar(/* janela */);
    Optional<BaixaBling> buscarBaixa(ChaveIdempotencia chave);
    ContaPagarExterna criarContaPagar(/* comando */);
    BaixaBling criarBordero(/* comando */);
}
```

Payloads `@HttpExchange` ficam no pacote `bling` e são mapeados para esses tipos.
Erros HTTP são traduzidos para exceções próprias: autenticação, limite, validação
remota, indisponibilidade e conflito.

### 9.2 Token

`BlingTokenService` fornece token válido e esconde renovação do restante do
sistema.

Regras:

- persistir access token, refresh token e `expires_at` no PostgreSQL;
- considerar expirado antes do prazo real usando margem configurável;
- manter um token por empresa e serializar o refresh com lock pessimista na linha
  correspondente;
- nunca registrar tokens em log;
- manter client ID e client secret apenas em variáveis de ambiente;
- se o refresh token for revogado, parar escritas e expor falha operacional
  clara no Actuator.

---

## 10. HTTP, Thymeleaf e HTMX

Não existe API JSON para a UI. Controllers recebem formulários validados e
retornam views ou fragmentos.

A aplicação usa `/page` como context path. Assim, a rota interna `/revisao` é
publicada como `https://conciliador.planteumaflor.com/page/revisao`. Thymeleaf e
HTMX devem gerar URLs relativas ao contexto, sem concatenar `/page` à mão.

Rotas iniciais:

| Método | Rota | Resultado |
|---|---|---|
| `GET` | `/entrar` | formulário de login |
| `GET/POST` | `/cadastro` | criação de empresa e acesso |
| `GET` | `/onboarding` | andamento das integrações |
| `POST` | `/integracoes/pluggy/conectar` | inicia conexão Pluggy |
| `GET` | `/integracoes/pluggy/retorno` | conclui conexão Pluggy |
| `GET` | `/inicio` | visão geral da empresa |
| `GET` | `/revisao` | página completa |
| `GET` | `/revisao/fila` | fragmento paginado da fila |
| `GET` | `/revisao/{id}` | detalhe/fragmento |
| `POST` | `/revisao/{id}/aprovar` | item atualizado |
| `POST` | `/revisao/{id}/classificar` | formulário validado + item |
| `POST` | `/revisao/{id}/match` | item atualizado |
| `POST` | `/revisao/{id}/ofx` | item atualizado |
| `POST` | `/revisao/{id}/retry` | item atualizado |
| `GET` | `/ofx/lotes/{id}/arquivo` | download OFX |
| `POST` | `/ofx/lotes/{id}/confirmar` | confirmação do upload |

Controllers não acessam repositórios diretamente. A fila usa uma query dedicada
e projeções `record`, evitando carregar agregados para leitura.

Validação falha com HTTP 422 e devolve o fragmento com erros de campo. Recurso
inexistente ou pertencente a outra empresa retorna 404; conflito de versão, 409;
violação de regra de domínio, 422. Uma `@ControllerAdvice` centraliza essa
tradução.

---

## 11. Segurança

- Form login protege toda a aplicação, exceto cadastro, login, recursos estáticos,
  callbacks estritamente necessários e `/actuator/health`.
- Usuários são persistidos no banco; senhas usam `PasswordEncoder` forte e nunca
  são armazenadas ou logadas em texto puro.
- A sessão contém o ID do usuário e da empresa autenticada. `empresa_id` enviado
  em URL, query string, formulário ou header é ignorado como fonte de autoridade.
- Repositórios de negócio expõem operações escopadas, por exemplo
  `findByIdAndEmpresaId(id, empresaId)`. Evitar `findById(id)` em fluxos de usuário.
- Usuário não autenticado recebe 401 em chamadas HTTP ou redirecionamento para
  login na navegação convencional.
- Recurso de outra empresa retorna 404 para não revelar sua existência.
- 403 é reservado para usuário autenticado sem permissão sobre uma operação da
  própria empresa; no v1 há um único perfil com acesso operacional completo.
- CSRF permanece habilitado. Formulários Thymeleaf/HTMX enviam o token.
- Sessão usa cookies `HttpOnly`, `SameSite=Lax` e `Secure` em produção.
- Login renova o identificador da sessão; logout invalida a sessão no servidor.
- Actuator expõe por HTTP somente `health` e `info`.
- Respostas e logs nunca incluem secrets, tokens ou payloads financeiros
  completos.

---

## 12. Configuração

`@ConfigurationProperties` valida toda configuração própria no startup.

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

server:
  servlet:
    context-path: /page

conciliador:
  timezone: America/Sao_Paulo
  ingest:
    cron: ${INGEST_CRON:0 0 4 * * *}
    dias-retroativos: ${INGEST_LOOKBACK_DAYS:7}
  classificacao:
    confianca-automatica: ${AUTO_CONFIDENCE:0.900}
    tolerancia-taxa: ${FEE_TOLERANCE:0.100}
  bling:
    base-url: ${BLING_BASE_URL:https://api.bling.com.br/Api/v3}
    client-id: ${BLING_CLIENT_ID}
    client-secret: ${BLING_CLIENT_SECRET}
  pluggy:
    base-url: ${PLUGGY_BASE_URL}
    api-key: ${PLUGGY_API_KEY}
```

As credenciais técnicas Pluggy/Bling são globais e ficam no ambiente. Conexões,
tokens OAuth, contas próprias, documentos e cursores de sincronização pertencem
à empresa e ficam no banco.

---

## 13. Scheduling e workers

- Scheduler de ingest: cron diário configurável, percorrendo integrações ativas
  sem misturar tenants.
- Classificação/match: `fixedDelay` curto, processando lotes limitados.
- Outbox: `fixedDelay` configurável e backoff persistido.
- Geração de OFX: sob demanda pela revisão, não por polling.
- `@EnableScheduling` fica em `SchedulingConfig`.

Cada job usa lock de banco nos registros, não lock global em memória. Mesmo com
uma instância, isso evita sobreposição entre execuções lentas.

No desligamento, a aplicação para de reivindicar eventos e termina o lote em
andamento dentro do timeout configurado.

---

## 14. Erros, auditoria e observabilidade

### 14.1 Erros

- Exceções de domínio não carregam detalhes HTTP.
- Clients traduzem falhas remotas antes de retornarem aos casos de uso.
- Mensagens persistidas são sanitizadas e limitadas em tamanho.
- Falha de um item não aborta o lote inteiro.

### 14.2 Auditoria

Registrar em tabela ou eventos de auditoria:

- quem aprovou ou alterou uma classificação;
- valor anterior e novo;
- match escolhido;
- rota API/OFX;
- timestamp e motivo.

O log técnico não substitui auditoria de negócio.

### 14.3 Métricas mínimas

- itens ingeridos, duplicados e rejeitados;
- fila por estado e idade do item mais antigo;
- classificação automática versus manual;
- matches automáticos versus revisão;
- outbox pendente, concluído, retries e falhas terminais;
- latência e erros por integração;
- validade restante do token Bling, sem expor o token.

Logs usam IDs de correlação e IDs internos. Descrição bancária, CPF/CNPJ e
payload integral não devem ser logados em nível normal.

---

## 15. Estratégia de testes

### 15.1 Unidade

- transições de `Transacao`;
- cálculo da taxa derivada;
- limites de `Confianca`;
- cada `RegraClassificacao` isoladamente;
- cada `EstrategiaMatch` isoladamente;
- normalização monetária;
- expiração e margem do token;
- backoff do outbox;
- geração OFX com fixtures determinísticas.

### 15.2 Integração

Com PostgreSQL Testcontainers e Flyway real:

- constraints e índices;
- isolamento entre duas empresas em todas as queries críticas;
- tentativa de associar transação, integração, outbox ou OFX entre empresas;
- idempotência concorrente do ingest;
- optimistic locking;
- reivindicação `SKIP LOCKED` sem duplicar trabalho;
- gravação atômica de estado + outbox;
- persistência e refresh concorrente do token;
- queries da fila.

### 15.3 HTTP

Com `MockMvc`:

- autenticação obrigatória;
- cadastro, login, logout e renovação de sessão;
- acesso cruzado entre empresas retorna 404;
- CSRF nos POSTs;
- fragments HTMX;
- validação 422;
- conflito 409;
- healthcheck liberado.

### 15.4 Clients externos

Usar servidor HTTP stub local para verificar método, path, headers e payload.
Não chamar Pluggy nem Bling reais na suíte automatizada.

---

## 16. Ordem de implementação

1. gerar projeto e configurar build, profiles e Testcontainers;
2. criar empresa, usuário, form login e testes de isolamento;
3. criar integração Pluggy e onboarding pós-cadastro;
4. criar `V3__transacao.sql` e o agregado `Transacao` com tenant obrigatório;
5. implementar normalizer e ingest idempotente por empresa;
6. implementar regras de classificação e fila básica;
7. implementar leitura Bling e estratégias de match;
8. criar token OAuth por empresa e refresh;
9. criar outbox e escrita idempotente no Bling;
10. criar revisão completa com HTMX;
11. criar lotes e arquivos OFX;
12. adicionar métricas, auditoria, build image e Compose.

Cada etapa deve deixar migrations, testes e observabilidade coerentes; não adiar
idempotência ou segurança para uma etapa final.

---

## 17. Critérios de aceite do backend v1

- Reprocessar a mesma janela Pluggy não duplica transações.
- Dois usuários de empresas diferentes nunca acessam dados um do outro.
- O tenant vem da sessão e todas as queries de usuário são escopadas por empresa.
- Recurso existente em outra empresa responde como 404.
- Cadastro cria empresa e usuário atomicamente.
- Conexão Pluggy ocorre após cadastro e fica vinculada à empresa.
- Nenhum `double` representa dinheiro.
- Hibernate valida o schema e não o altera.
- Toda mudança de estado passa por método do agregado.
- Crédito de venda é casado antes de qualquer tentativa de criação.
- Transferência interna nunca é contabilizada como receita ou despesa.
- Escrita no Bling ocorre exclusivamente pelo worker de outbox.
- Retry de escrita verifica existência antes de novo POST.
- Token Bling sobrevive a restart e é renovado de forma serializada.
- Itens ambíguos ficam disponíveis na fila autenticada.
- Ações HTMX preservam CSRF e tratam conflito de versão.
- OFX só é marcado conciliado após confirmação humana.
- Testes de persistência executam contra PostgreSQL real.
- Healthcheck distingue banco indisponível e autenticação Bling inválida.

---

## 18. Decisões pendentes

1. Se o cadastro será público ou dependerá de convite/administração.
2. Estratégia de verificação de e-mail e recuperação de senha.
3. Janela de datas e horários exata para cada conta Pluggy.
4. Critério de identificação de baixa existente no Bling.
5. Payload confirmado de borderô para líquido + taxa.
6. Endpoint/estratégia final para transferência interna.
7. Formato de armazenamento da auditoria de negócio.
8. Política de retenção de transações, outbox, auditoria e arquivos OFX.
9. Se tokens Pluggy/Bling exigem criptografia em repouso no banco.
