# Próximos passos do Conector

## Resumo

A branch `dev` já contém identidade, onboarding inicial, domínio de transações,
integração Cora direta, classificação básica e perfil. O roadmap antigo que
apontava `Transacao` como próximo passo ficou obsoleto após os merges de
2026-06-24.

### Decisão de conectores

- **Cora direto** é o primeiro conector bancário operacional e a prioridade
  imediata de estabilização.
- **Pluggy permanece no escopo** como agregador para outras contas. A fundação
  atual é fake e será evoluída sem acoplar o domínio `Transacao` ao fornecedor.
- Toda transação mantém `fonte` e unicidade por
  `(empresa_id, fonte, id_transacao_externa)`.

## Ordem recomendada

1. **Restabelecer a validação do build**
   - Garantir Java 21, Docker disponível para Testcontainers e executar
     `./mvnw test` (`mvnw.cmd test` no Windows).
   - Em 2026-06-24, a compilação estática de fontes e testes passou e os 22
     testes unitários executáveis sem infraestrutura ficaram verdes. Os 47
     métodos de integração ainda dependem do Docker/Testcontainers.

2. **Consolidar ingestão Cora — concluído em 2026-06-24**
   - Agendar sincronização por cron e executar retry limitado com backoff.
   - Persistir saúde e falhas consecutivas sem armazenar payload ou segredo.
   - Tornar o insert idempotente atômico com `ON CONFLICT DO NOTHING`, evitando
     a corrida de `exists` seguido de `insert`.
   - Manter sincronização manual autenticada e isolamento por tenant.

3. **Fechar classificação e fila básica — concluído em 2026-06-24**
   - Normalizar descrições, ampliar regras determinísticas e enviar qualquer
     caso abaixo do limiar para `EM_REVISAO`.
   - Expor uma fila autenticada, paginada e exclusivamente escopada pela empresa.
   - Evoluir depois para os comandos, detalhes e fragments HTMX da Tela 05.

4. **Entregar a Tela 04 — Início**
   - Criar contagens por estado no banco, saúde das integrações, última
     sincronização, atividade recente e ação `Sincronizar agora`.
   - Remover o placeholder atual.

5. **Implementar o núcleo Bling**
   - Leitura e estratégias de match → OAuth/refresh por empresa → outbox e
     escrita idempotente.

6. **Completar os fluxos do v1**
   - Revisão HTMX completa → lotes OFX → Pluggy real → métricas/auditoria →
     build image e CI. Ordem de dependências: [Backend §16](./Backend.md).

## Testes de aceite imediatos

- Suíte completa verde com PostgreSQL/Testcontainers.
- Sincronização repetida ou concorrente não duplica transações.
- Falha do Cora atualiza a saúde sem expor credenciais; sucesso posterior
  recupera a integração.
- Transações e fila de revisão permanecem isoladas entre empresas.
- Casos ambíguos entram em `EM_REVISAO`; regras seguras ficam `CLASSIFICADO`.
- Transições inválidas e alterações após `CONCILIADO` falham.

## Premissas

- Cadastro público permanece no v1.
- Hardening de rate limit e refinamentos visuais ficam após o núcleo funcional.
- Nenhuma migration Flyway já versionada será reescrita.
