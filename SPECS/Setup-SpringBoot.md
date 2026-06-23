# Conciliador — Setup Spring Boot (detalhamento de implementação)

> **Status:** guia de bootstrap v1 · concretiza ARQUITETURA §11 e Backend §3–§16.
> **Objetivo:** sair da fase de specs para um esqueleto compilável, com build,
> migrations, segurança e Testcontainers já no lugar — sem antecipar regra de
> negócio.

Este documento é o **passo 1** da ordem de implementação do Backend §16.

---

## 1. Geração do projeto

Gerar pelo **Spring Initializr** (start.spring.io) ou Maven:

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.x (última estável da linha) |
| Group | `com.planteumaflor` |
| Artifact | `conciliador` |
| Package name | `com.planteumaflor.conciliador` |
| Packaging | Jar |
| Java | 21 |

Dependências iniciais: **Spring Web**, **Thymeleaf**, **Spring Data JPA**,
**PostgreSQL Driver**, **Flyway Migration**, **Validation**, **Spring Security**,
**Actuator**, **Testcontainers**.

> Sem Lombok (Backend §2). DTOs/commands/projeções são `record` do Java 21.

---

## 2. `pom.xml` — dependências-chave

Além das geradas pelo Initializr, adicionar os WebJars (HTMX, Bootstrap) e o
suporte declarativo de HTTP client:

```xml
<!-- WebJars: HTMX e Bootstrap servidos localmente, nunca CDN (Frontend §3) -->
<dependency>
  <groupId>org.webjars.npm</groupId>
  <artifactId>htmx.org</artifactId>
  <version>__fixar__</version>
</dependency>
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>bootstrap</artifactId>
  <version>__fixar__</version>
</dependency>
<dependency>
  <groupId>org.webjars</groupId>
  <artifactId>webjars-locator-core</artifactId>
</dependency>

<!-- Testes: Testcontainers PostgreSQL + Security -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-testcontainers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

Os clients **Pluggy** e **Bling** usam `@HttpExchange` sobre `RestClient`
(Backend §11.4 / §8 / §9), recurso nativo do Spring — sem dependência extra.
Versões dos WebJars ficam **fixadas** (Frontend §3 exige offline e versão presa).

---

## 3. Estrutura de pacotes (do Backend §4, em forma de checklist)

Topo por feature; subpacotes só quando a responsabilidade existe de fato:

```text
com.planteumaflor.conciliador
├── ConciliadorApplication.java
├── identidade   (domain · application · web · persistence · security)
├── empresa      (domain · persistence)
├── pluggy       (domain · application · client · persistence · web)
├── onboarding   (application · web)
├── inicio       (query · web)
├── integracoes  (query · web)
├── ingest       (application · scheduling)
├── transacao    (domain · query · persistence · web)
├── classificacao(domain · application[/regras])
├── match        (domain · application[/estrategias])
├── bling        (domain · application · client · persistence · web)
├── outbox       (domain · application · persistence · scheduling)
├── ofx          (domain · application · query · persistence · web)
├── revisao      (application · query · web)
└── config       (SecurityConfig · SchedulingConfig · ClockConfig · ConciliadorProperties)
```

Regras de visibilidade (Backend §4): **sem** pacotes globais `controller`/
`service`/`repository`; **sem** `BaseController`/`BaseService`/`BaseRepository`;
implementações com menor visibilidade possível; `web → application/query →
domain`; `client`/`persistence` implementam portas do `domain`.

---

## 4. `application.yml` (Backend §12, completo)

```yaml
spring:
  application:
    name: conciliador
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate      # Hibernate só valida; Flyway é dono do schema
    open-in-view: false       # nada de lazy loading na view
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
  threads:
    virtual:
      enabled: true           # workers I/O-bound (Pluggy/Bling) em virtual threads

server:
  servlet:
    context-path: /page       # rota interna /revisao = .../page/revisao
  forward-headers-strategy: framework   # atrás de proxy (HTTPS, host)

management:
  endpoints:
    web:
      exposure:
        include: health,info  # só health e info via HTTP (Backend §11)
  endpoint:
    health:
      probes:
        enabled: true

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
    margem-expiracao: ${BLING_TOKEN_SKEW:PT2M}
  pluggy:
    base-url: ${PLUGGY_BASE_URL}
    # Sem api-key global: cada empresa usa o seu Meu Pluggy. As credenciais
    # (clientId/clientSecret) ficam no banco, por empresa, criptografadas.
  cripto:
    chave: ${CRIPTO_KEY}   # chave p/ cifrar credenciais Pluggy e refresh token Bling
```

`ConciliadorProperties` (`@ConfigurationProperties("conciliador")` +
`@Validated`) valida tudo no startup (Backend §12). Segredos só por ambiente; em
dev, `.env` no `.gitignore` (ARQUITETURA §11.9).

---

## 5. Migrations Flyway (Backend §7.1) — esqueleto

`src/main/resources/db/migration/`, **todas com `empresa_id`** (ver
AUDITORIA-consistencia §2.1):

```text
V1__empresa_e_usuario.sql            -- empresa, usuario (email único, senha hash)
V2__integracao_pluggy_e_conta_bancaria.sql   -- inclui credenciais Meu Pluggy por empresa (cifradas)
V3__transacao.sql                    -- §4 da ARQUITETURA, COM empresa_id
V4__outbox_bling.sql                 -- inclui tentativas/erro_ultimo (Backend §7.2)
V5__bling_oauth_token.sql            -- 1 token por empresa, @Version
V6__lote_ofx.sql
V7__auditoria.sql                    -- quem/quando/valor anterior/novo (Backend §14.2)
V8__indices_workers.sql              -- índices parciais p/ estados pendentes
```

Pontos obrigatórios das migrations:

- `transacao`: `UNIQUE (empresa_id, pluggy_transaction_id)`; `valor_liquido
  NUMERIC(14,2) NOT NULL`; índice de match começando por `empresa_id`.
- `outbox_bling`: `UNIQUE (empresa_id, chave_idempotencia)`; `payload JSONB`;
  `proxima_tentativa`, `locked_at` (claim com `FOR UPDATE SKIP LOCKED`).
- Índices parciais: ex. `CREATE INDEX ... ON outbox_bling (proxima_tentativa)
  WHERE status = 'PENDENTE';` e equivalente para `transacao(estado)` nos estados
  ativos.
- Timestamps `TIMESTAMPTZ` (UTC); datas bancárias `DATE`.

`ddl-auto=validate` em **todos** os ambientes, inclusive testes (Backend §7.1).

---

## 6. Entidades e dinheiro

- `Transacao` é entidade JPA **e** objeto de domínio (Backend §5.1): sem setters
  públicos; mudanças de estado por métodos (`classificar`, `aprovarParaApi`,
  `conciliar`...). `@Version` para optimistic locking.
- Enums persistidos com `@Enumerated(EnumType.STRING)`.
- Dinheiro: `BigDecimal` escala 2, `RoundingMode.HALF_EVEN` na borda; comparar com
  `compareTo`, nunca `equals` (Backend §5.2). `Direcao` carrega o sinal; o valor é
  sempre positivo.
- `Confianca` value object (0.000–1.000, escala 3).

---

## 7. Segurança (Backend §11) — `SecurityConfig`

- **Form login** + sessão + CSRF habilitado.
- Liberar: `/entrar`, `/cadastro`, estáticos (`/css/**`, `/js/**`, `/webjars/**`,
  `/img/**`), callbacks externos estritamente necessários e `/actuator/health`.
  Todo o resto exige autenticação.
- `PasswordEncoder` forte (`DelegatingPasswordEncoder`/BCrypt).
- `UsuarioDetailsService` (em `identidade.security`) carrega por e-mail
  normalizado; `UsuarioPrincipal` expõe `usuarioId` e `empresaId` imutáveis.
- `LoginSuccessHandler` decide onboarding vs início vs saved request (tela 01).
- Cookies `HttpOnly`, `SameSite=Lax`, `Secure` em prod; renovar id de sessão no
  login; logout = POST com CSRF.
- **Tenant nunca vem do request.** Um `@AuthenticationPrincipal`/resolver injeta
  `EmpresaId` da sessão nos controllers (telas 04–07).

> Importante: os webhooks/callbacks (Pluggy/Bling) ficam em controllers próprios,
> fora do form login, com validação de assinatura/`state` própria (telas 03/07).

---

## 8. Clients HTTP declarativos (Backend §8, §9, §11.4)

```java
@HttpExchange(url = "/", accept = "application/json")
interface BlingClient {
    @GetExchange("/contas/receber")
    BlingPage<ContaReceberDto> contasReceber(@RequestParam Map<String,?> filtros);
    // ... contasPagar, postBordero, postContaPagar
}
```

- Registrar via `RestClient` + `HttpServiceProxyFactory` em
  `BlingClientConfig`/`PluggyClientConfig`.
- DTOs externos **não escapam** dos pacotes `bling`/`pluggy`/`ingest`; o
  normalizer converte para o modelo canônico na borda.
- Traduzir erros HTTP em exceções de domínio (auth, rate limit, validação remota,
  indisponibilidade, conflito). Respeitar `Retry-After` em 429/5xx (Backend §8).

---

## 9. Scheduling e workers (Backend §13)

- `@EnableScheduling` em `SchedulingConfig`.
- Ingest: `@Scheduled(cron = "${conciliador.ingest.cron}")`, zona
  `America/Sao_Paulo`, percorre integrações ativas por empresa.
- Classificação/match e outbox: `fixedDelay` curto, lotes limitados, claim com
  `FOR UPDATE SKIP LOCKED`.
- OFX: sob demanda (sem polling).
- `Clock` injetado (`ClockConfig`) para expiração/janela/backoff testáveis.
- Nenhuma chamada HTTP dentro de transação de banco aberta (Backend §2.7).

---

## 10. Build de imagem e Compose (ARQUITETURA §11.8)

- Imagem por **Cloud Native Buildpacks**: `./mvnw spring-boot:build-image`. Sem
  Dockerfile próprio.
- `compose.yaml` com dois serviços: a aplicação e `postgres:16`.

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: conciliador
      POSTGRES_USER: ${DATABASE_USER}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - conciliador-db:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DATABASE_USER} -d conciliador"]
      interval: 5s
      timeout: 3s
      retries: 10
  app:
    image: conciliador:latest
    depends_on:
      db:
        condition: service_healthy
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/conciliador
      # ... demais variáveis do §4
    ports: ["8080:8080"]
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/page/actuator/health"]
      interval: 10s
      timeout: 3s
      retries: 5
volumes:
  conciliador-db:
```

---

## 11. Testes desde o início (Backend §15)

- **Integração:** `@SpringBootTest` + Testcontainers com `@ServiceConnection`
  (PostgreSQL real, **nunca H2** — semântica monetária/SQL importa). Flyway roda
  nos testes.
- Cobrir já no esqueleto: isolamento entre **duas empresas**, idempotência
  concorrente do ingest, optimistic locking, claim `SKIP LOCKED`.
- **HTTP:** `MockMvc` + `spring-security-test` para auth obrigatória, 404
  cross-tenant, CSRF nos POSTs, fragments HTMX, 422/409.
- **Clients externos:** stub HTTP local (ex. `MockWebServer`) — não chamar Pluggy
  nem Bling reais (Backend §15.4). Espelhar os 5 pontos de homologação do
  documento Bling-API-v3 §7.

---

## 12. Definition of done do esqueleto (passo 1)

- [ ] Projeto compila e sobe com `compose.yaml` (app + postgres healthy).
- [ ] `ddl-auto=validate` passa contra o schema do Flyway.
- [ ] `SecurityConfig` protege tudo menos as rotas liberadas; `/actuator/health`
      aberto.
- [ ] `ConciliadorProperties` valida config no startup.
- [ ] Um teste de integração com Testcontainers verde.
- [ ] Um teste `MockMvc` de auth obrigatória verde.
- [ ] `.env` no `.gitignore`; nenhum segredo versionado.

Concluído isto, seguir para o passo 2 do Backend §16 (empresa, usuário, form
login e testes de isolamento).
