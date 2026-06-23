# Conciliador Plante Uma Flor

Aplicação web para centralizar, classificar e conciliar as movimentações
financeiras da Plante Uma Flor.

O sistema coleta transações bancárias pelo Pluggy, identifica sua natureza e
prepara a conciliação no Bling. Casos seguros seguem automaticamente; situações
ambíguas são encaminhadas para uma fila de revisão acessível pelo proprietário ou
contador.

## Objetivo geral

Reduzir o trabalho manual e os erros na conciliação financeira diária, mantendo
controle sobre decisões que afetam caixa, receitas, despesas e transferências
internas.

O Conciliador busca evitar principalmente:

- transferências entre contas próprias registradas como receita ou despesa;
- vendas e recebimentos contabilizados em duplicidade;
- uso do valor bruto quando somente o valor líquido entrou no caixa;
- despesas classificadas em categorias incorretas;
- reenvio de baixas já registradas no Bling;
- transações esquecidas por falhas ou ambiguidades de integração.

## Como funciona

```text
Pluggy
   ↓
Ingestão e normalização
   ↓
Classificação e busca de correspondências no Bling
   ↓
┌───────────────────┬────────────────────┬─────────────────┐
│ Escrita automática│ Fila de revisão    │ Geração de OFX  │
│ via API do Bling  │ proprietário/cont. │ para upload     │
└───────────────────┴────────────────────┴─────────────────┘
```

Cada transação mantém seu histórico e estado durante todo o processo. A escrita
no Bling é desacoplada da importação para permitir idempotência, retry e
recuperação segura de falhas.

## Funcionalidades previstas

- cadastro de empresa e acesso autenticado;
- conexão de contas bancárias pelo Pluggy após o cadastro;
- visão consolidada das transações, com filtros por conta bancária;
- classificação automática por regras e nível de confiança;
- identificação de vendas, despesas e transferências internas;
- busca de contas a receber e pagar correspondentes no Bling;
- fila de revisão para decisões ambíguas;
- escrita idempotente no Bling por outbox;
- geração e acompanhamento de lotes OFX;
- histórico e auditoria das decisões;
- interface responsiva para computador e celular;
- isolamento completo dos dados entre empresas.

## Princípios do produto

- **O caixa usa o valor líquido:** dinheiro é representado por `BigDecimal` e
  `NUMERIC(14,2)`, nunca por `double`.
- **Casa antes de criar:** o sistema procura registros existentes no Bling antes
  de criar novos lançamentos.
- **Transferência não é receita nem despesa:** movimentações entre contas próprias
  recebem tratamento específico.
- **Automação com controle:** somente decisões de alta confiança são automáticas.
- **Integrações idempotentes:** repetir ingestão ou processamento não pode gerar
  duplicidade.
- **Segurança por tenant:** um usuário nunca acessa dados de outra empresa.
- **Flyway controla o schema:** Hibernate apenas valida o banco.
- **Feature primeiro:** o código é organizado por domínio, com responsabilidades
  internas bem delimitadas.

## Escopo do v1

O primeiro lançamento será uma aplicação Spring Boot executada em uma única
instância, com:

- Java 21, Maven e PostgreSQL 16;
- Spring MVC, Thymeleaf, HTMX e Bootstrap (via WebJar);
- Spring Data JPA e Flyway;
- autenticação por formulário e sessão;
- Pluggy para agregação bancária;
- Bling API v3 com OAuth 2.0;
- Testcontainers para testes com PostgreSQL real;
- imagem gerada por Cloud Native Buildpacks.

Não fazem parte do v1 uma API pública, aplicação SPA, execução em cluster ou
conciliação completamente automática de casos ambíguos.

## Status

A implementação está em andamento na branch `dev` (passos 1–3). Login e
cadastro foram corrigidos conforme o contrato das telas 01 e 02, com 35 testes
verdes. Os próximos passos — passo 4 (domínio de transações) em diante — estão
em [`SPECS/PROXIMOS-PASSOS.md`](./SPECS/PROXIMOS-PASSOS.md).

## Documentação

- [Arquitetura geral](./SPECS/ARQUITETURA-conciliador.md)
- [Especificação do backend](./SPECS/Backend.md)
- [Especificação do frontend](./SPECS/Frontend.md)

Guias complementares:

- [Próximos passos](./SPECS/PROXIMOS-PASSOS.md)
- [Auditoria de consistência das specs](./SPECS/AUDITORIA-consistencia.md)
- [API v3 do Bling — decisões](./SPECS/Bling-API-v3.md)
- [Setup Spring Boot](./SPECS/Setup-SpringBoot.md)
- [Frontend — implementação](./SPECS/Frontend-Implementacao.md)
- [Correções de implementação por tela](./SPECS/implementação/README.md)

Telas:

- [Login](./SPECS/TELAS/01-Login.md)
- [Cadastro](./SPECS/TELAS/02-Cadastro.md)
- [Onboarding](./SPECS/TELAS/03-Onboarding.md)
- [Início](./SPECS/TELAS/04-Inicio.md)
- [Fila de revisão](./SPECS/TELAS/05-Revisao.md)
- [Transações](./SPECS/TELAS/06-Transacoes.md)
- [Integrações](./SPECS/TELAS/07-Integracoes.md)
- [Lotes OFX](./SPECS/TELAS/08-Lotes-OFX.md)
- [Perfil](./SPECS/TELAS/09-Perfil.md)

