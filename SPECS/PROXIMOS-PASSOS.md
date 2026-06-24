# Próximos passos do Conector

## Resumo

A branch `dev` está limpa e contém os passos 1–3. A prioridade é estabilizar login/cadastro e depois iniciar o passo 4: o domínio de transações.

## Ordem recomendada

1. **Restabelecer a validação do build**
   - Versionar `mvnw` como executável.
   - Garantir Java 21/JAVA_HOME e executar `./mvnw test`.
   - Neste ambiente, os testes não puderam rodar por ausência do Java.

2. **Corrigir login e cadastro**
   - Login: saved request seguro, redirecionamento de usuário autenticado e sessão expirada.
   - Cadastro: auto-login para `/onboarding`, nunca reapresentar senhas e retornar HTTP 422 em erros.
   - Adicionar testes MockMvc desses fluxos.
   - Referência: [pendências das telas](./implementação/README.md).

3. **Implementar o passo 4 — Transação**
   - Criar `V3__transacao.sql`, sempre com `empresa_id`.
   - Implementar `Transacao`, `EstadoTransacao`, `ClasseTransacao`, `Direcao` e `Confianca`.
   - Garantir valor positivo com `BigDecimal`, unicidade por empresa, `@Version` e transições de estado protegidas.
   - Criar repositório com consultas obrigatoriamente escopadas pelo tenant.
   - Referência: [contrato do backend](./Backend.md).

4. **Completar a base Pluggy antes do ingest**
   - Manter o fake para testes.
   - Em nova migration posterior à V3, adicionar credenciais cifradas e contas bancárias sem alterar a V2 já aplicada.
   - Implementar adapter real e evolução das etapas do onboarding.

5. **Continuar o roadmap**
   - Ingest idempotente → classificação/fila básica → leitura e match Bling → OAuth → outbox → revisão HTMX → OFX → métricas/auditoria.
   - A tela Início deixa de ser placeholder depois que `Transacao` oferecer as contagens.
   - Ordem oficial: [Backend §16](./Backend.md).

## Testes de aceite imediatos

- Suíte completa verde com PostgreSQL/Testcontainers.
- Login preserva somente redirects internos.
- Cadastro termina autenticado e nunca devolve a senha no HTML.
- Transações de empresas diferentes permanecem isoladas.
- Duplicidade `(empresa_id, pluggy_transaction_id)` é rejeitada.
- Transições inválidas e alterações após `CONCILIADO` falham.

## Premissas

- Cadastro público permanece no v1.
- Hardening de rate limit e refinamentos visuais ficam após o núcleo funcional.
- Nenhuma migration Flyway já versionada será reescrita.
