# Próximos passos do Conector

## Resumo

A branch `dev` já contém: identidade completa (login, cadastro, perfil, alterar
senha), onboarding com Cora e Pluggy reais, credenciais por empresa cifradas
(`CriptoService`), contas bancárias, ingestão e classificação de transações,
fila de revisão, tela de Integrações, dashboard Início, lotes OFX e templates
para todas as telas. Migrações V1–V10 aplicadas.

### Decisão de conectores

- **Cora direto** é o primeiro conector bancário operacional.
- **Pluggy** tem adapter real (`PluggyGatewayAdapter`); credenciais por empresa
  criptografadas no banco (V9).
- Toda transação mantém `fonte` e unicidade por
  `(empresa_id, fonte, id_transacao_externa)`.

## O que ainda falta

1. **Restabelecer a validação do build**
   - Garantir Java 21, Docker disponível para Testcontainers e executar
     `mvnw.cmd test` no Windows.
   - Testes unitários passam sem infraestrutura; testes de integração dependem
     do Docker/Testcontainers.

2. **Implementar o núcleo Bling**
   - [x] OAuth (authorization code) + refresh por empresa → `BlingToken`
     persistido e cifrado (migration V15) → `BlingTokenService` com renovação
     serializada (lock pessimista) e revogação como estado terminal →
     `BlingOAuthController` (`POST /integracoes/bling/conectar`,
     `GET /integracoes/bling/retorno`) com `state` assinado de uso único →
     card na tela de Integrações → `BlingHealthIndicator` no Actuator.
   - [ ] `BlingGateway`/`BlingClient`: leitura de contas a receber/pagar →
     estratégias de match → outbox e escrita idempotente (borderô).
     Depende de confirmar no sandbox o schema do POST de borderô e os filtros
     de GET (ver [Bling-API-v3 §7](./Bling-API-v3.md)).
   - Ver [Backend §16](./Backend.md) passos 7–9.

3. **Completar revisão HTMX e OFX**
   - Comandos de revisão (`aprovar`, `classificar`, `match`, `ofx`, `retry`)
     com fragmentos HTMX e proteção de versão (409).
   - Confirmação humana de upload OFX → estado `CONCILIADO`.

4. **Finalizar onboarding**
   - Ampliar `EtapaOnboarding` para as 7 etapas do spec.
   - Fluxo OAuth Bling (`/integracoes/bling/conectar` + retorno).
   - Polling HTMX de primeira sincronização.
   - Testes: callback state inválido, idempotência, isolamento.

5. **Métricas, auditoria e CI**
   - Tabela ou eventos de auditoria de classificação/match.
   - Métricas Actuator (fila por estado, outbox, latência por integração).
   - Build image Docker e pipeline CI.

## Testes de aceite pendentes

- Suíte completa verde com PostgreSQL/Testcontainers.
- Sincronização repetida ou concorrente não duplica transações (Cora e Pluggy).
- Falha de conector atualiza saúde sem expor credenciais.
- Transações, fila e OFX permanecem isolados entre empresas.
- Transições inválidas e alterações após `CONCILIADO` falham.
- Revisão via HTMX preserva CSRF e trata conflito de versão (409).
- OFX marcado `CONCILIADO` somente após confirmação humana.

## Premissas

- Cadastro público permanece no v1.
- Hardening de rate limit e refinamentos visuais ficam após o núcleo funcional.
- Nenhuma migration Flyway já versionada será reescrita.
