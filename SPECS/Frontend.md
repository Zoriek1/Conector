# Conciliador — Especificação do frontend

> **Status:** proposta v0.1  
> **Documentos relacionados:** [Arquitetura](./ARQUITETURA-conciliador.md) ·
> [Backend](./Backend.md)  
> **URL prevista:** `https://conciliador.planteumaflor.com/page`

> **Precedência:** este documento substitui a decisão anterior de HTTP Basic. A
> interface usa cadastro, form login e sessão persistida pelo backend.

---

## 1. Objetivo

Entregar uma interface operacional para cadastrar a empresa, conectar as
integrações, acompanhar a sincronização e revisar cerca de 20 transações por dia.
A experiência deve funcionar em computador e celular, com prioridade para uso em
desktop.

O frontend é server-rendered com Thymeleaf. HTMX atualiza apenas os fragmentos
necessários; não haverá SPA nem API JSON exclusiva para a interface.

---

## 2. Premissas de produto

- Cada cadastro representa uma empresa e cria um único acesso no v1.
- O acesso pode ser usado pelo proprietário ou contador, com as mesmas permissões
  operacionais.
- Uma empresa nunca visualiza dados de outra.
- Cada empresa pode conectar várias contas bancárias pelo Pluggy.
- Transações de todas as contas aparecem em uma lista consolidada.
- Conta bancária é filtro da lista, não uma página ou tabela isolada.
- A integração Pluggy é configurada após o cadastro, dentro do onboarding.
- Ações financeiras ambíguas exigem confirmação humana.

---

## 3. Stack frontend

- Thymeleaf para páginas e fragments.
- HTMX por WebJar, sem CDN.
- Bootstrap por WebJar para grid, formulários, componentes responsivos e
  acessibilidade básica.
- CSS próprio com variáveis de tema para identidade da Plante Uma Flor.
- JavaScript próprio somente quando o HTMX não resolver, principalmente para o
  fluxo embarcado do Pluggy e pequenos comportamentos de interface.
- Ícones locais por WebJar ou SVG versionado no projeto.

Não incluir React, Vue, Angular, jQuery, Tailwind CLI ou pipeline Node no v1.

---

## 4. Arquitetura de informação

### 4.1 Área pública

```text
/entrar
/cadastro
/recuperar-senha        futuro, antes da abertura pública
```

### 4.2 Área autenticada

```text
/onboarding
/inicio
/revisao
/transacoes
/integracoes
/ofx/lotes
/perfil
```

Todas as rotas são relativas ao context path `/page`. Templates e atributos HTMX
geram URLs com suporte ao contexto; nenhuma view concatena `/page` manualmente.

### 4.3 Specs por tela

| # | Tela | Especificação detalhada |
|---|---|---|
| 1 | Login | [01-Login.md](./TELAS/01-Login.md) |
| 2 | Cadastro | [02-Cadastro.md](./TELAS/02-Cadastro.md) |
| 3 | Onboarding | [03-Onboarding.md](./TELAS/03-Onboarding.md) |
| 4 | Início | [04-Inicio.md](./TELAS/04-Inicio.md) |
| 5 | Fila de revisão | [05-Revisao.md](./TELAS/05-Revisao.md) |
| 6 | Transações | [06-Transacoes.md](./TELAS/06-Transacoes.md) |
| 7 | Integrações | [07-Integracoes.md](./TELAS/07-Integracoes.md) |
| 8 | Lotes OFX | [08-Lotes-OFX.md](./TELAS/08-Lotes-OFX.md) |
| 9 | Perfil | [09-Perfil.md](./TELAS/09-Perfil.md) |

Cada spec define função, controller, interfaces de aplicação, view models,
composição Thymeleaf, encapsulamento, HTMX, segurança e testes.

---

## 5. Navegação e app shell

### 5.1 Desktop

Usar sidebar fixa e área principal fluida.

Itens:

1. Início.
2. Revisão, com badge de pendências.
3. Transações.
4. Lotes OFX.
5. Integrações.
6. Perfil.

O rodapé da sidebar mostra empresa, e-mail autenticado e ação de sair.

### 5.2 Mobile

Usar cabeçalho compacto e navegação inferior com os quatro destinos mais usados:

- Início;
- Revisão;
- Transações;
- Menu.

O menu secundário contém Integrações, OFX, Perfil e Sair. A navegação não depende
de hover.

### 5.3 Cabeçalho da página

Cada página possui:

- título único;
- descrição curta quando necessária;
- estado de sincronização;
- ação primária contextual;
- breadcrumbs somente em fluxos com mais de um nível.

---

## 6. Cadastro e autenticação

### 6.1 Cadastro

Campos iniciais:

- nome da empresa;
- nome do responsável;
- e-mail;
- senha;
- confirmação da senha;
- aceite dos termos e política de privacidade, quando existirem.

Cadastro de empresa e usuário ocorre atomicamente. Após sucesso, iniciar sessão e
redirecionar para `/onboarding`; não pedir novo login.

Requisitos da tela:

- erros junto ao campo;
- resumo acessível de erros no topo;
- medidor textual dos requisitos da senha;
- botão bloqueado durante envio;
- mensagem genérica quando o e-mail já estiver cadastrado, sem expor mais dados.

### 6.2 Login

Campos: e-mail e senha. A tela oferece cadastro e, quando implementado,
recuperação de senha.

Após autenticar:

- empresa sem Pluggy conectado vai para `/onboarding`;
- empresa configurada vai para `/inicio`;
- URL interna originalmente solicitada pode ser restaurada se pertencer à mesma
  sessão e for segura.

### 6.3 Sessão expirada

Navegação comum redireciona para `/entrar`. Em uma requisição HTMX, o backend
envia redirecionamento HTMX para a mesma tela. Mensagens nunca afirmam que um
recurso de outra empresa existe.

---

## 7. Onboarding e integrações

O onboarding é apresentado após o cadastro e pode ser retomado.

### 7.1 Etapas

1. **Conta criada:** exibir empresa e responsável.
2. **Configurar Meu Pluggy:** a empresa informa as credenciais do seu próprio Meu
   Pluggy (clientId/clientSecret); o backend valida e guarda por empresa,
   criptografadas.
3. **Conectar bancos:** iniciar o fluxo do widget Pluggy com as credenciais da
   empresa.
4. **Confirmar contas:** listar contas descobertas e habilitá-las para ingestão.
5. **Conectar Bling:** concluir OAuth para permitir match e escrita.
6. **Primeira sincronização:** exibir progresso e resultado.

Cada empresa usa o seu próprio Meu Pluggy: as credenciais ficam no banco (por
empresa, criptografadas), nunca em `.env`. O frontend inicia o fluxo no backend; o
backend vincula o resultado à empresa autenticada e nunca expõe as credenciais ao
navegador.

### 7.2 Estados de uma integração

```text
NÃO_CONECTADA
CONECTANDO
SINCRONIZANDO
ATIVA
REQUER_ATENÇÃO
DESCONECTADA
```

Cada card de integração exibe:

- provedor;
- estado textual e ícone;
- última sincronização;
- contas encontradas;
- mensagem de ação necessária;
- conectar, reconectar ou remover.

Remover exige modal de confirmação e explica o efeito. A remoção da conexão não
apaga automaticamente o histórico financeiro.

### 7.3 Primeira sincronização

Após concluir o Pluggy:

- mostrar progresso sem prometer atualização instantânea;
- permitir sair da tela e continuar em background;
- atualizar o card por polling HTMX moderado;
- mostrar quantas contas e transações foram importadas;
- oferecer acesso à fila quando houver itens para revisão.

---

## 8. Página inicial

Objetivo: responder rapidamente “o sistema está funcionando?” e “o que exige
minha atenção?”.

### 8.1 Resumo

Cards:

- aguardando revisão;
- falhas de escrita;
- aguardando API;
- aguardando OFX;
- conciliadas no período.

### 8.2 Integrações

Faixa compacta com estado do Pluggy, Bling e horário da última sincronização. Um
problema deve ter texto acionável, por exemplo “Reconecte o Cora”, não apenas um
ícone vermelho.

### 8.3 Atividade recente

Mostrar as últimas transações e ações de revisão. O dashboard não replica toda a
fila; oferece links com filtros prontos.

---

## 9. Fila de revisão

É a tela central do produto.

### 9.1 Filtros

- busca por descrição, documento ou identificador;
- período;
- conta bancária: Todas, Cora, Stone, Mercado Pago e demais conectadas;
- direção: crédito ou débito;
- classe sugerida;
- confiança;
- motivo da revisão;
- estado.

Filtros são representados na query string, podem ser compartilhados dentro da
mesma conta e sobrevivem ao refresh. Aplicar filtro atualiza lista, totais e URL
com `hx-push-url`.

### 9.2 Tabela desktop

Colunas:

| Coluna | Conteúdo |
|---|---|
| Data | data bancária |
| Conta | instituição e final identificador |
| Descrição | texto normalizado + acesso ao original |
| Direção | crédito/débito com texto e ícone |
| Valor | BRL, alinhado à direita |
| Sugestão | classe proposta |
| Confiança | percentual e categoria textual |
| Motivo | razão da revisão |
| Estado | badge textual |
| Ação | revisar |

A linha selecionada abre painel lateral sem abandonar filtros e paginação.

### 9.3 Cards mobile

No celular, cada item apresenta:

- valor e direção;
- data e conta;
- descrição em até duas linhas;
- sugestão, confiança e estado;
- botão “Revisar”.

Não usar rolagem horizontal como experiência principal.

### 9.4 Painel de detalhe

Conteúdo:

- dados bancários completos;
- descrição original;
- classificação e justificativa;
- candidatos de match ordenados;
- conta Bling, valor bruto, líquido e taxa derivada;
- histórico/auditoria;
- avisos de risco.

Ações:

- aprovar sugestão;
- alterar classificação;
- escolher outro match;
- criar despesa quando permitido;
- enviar para OFX;
- tentar novamente após falha.

A ação primária deve ser inequívoca. Operações com impacto diferente não ficam
visualmente equivalentes.

### 9.5 Pós-ação

Após aprovar:

- atualizar ou remover a linha;
- atualizar os totais;
- fechar o painel ou abrir o próximo item;
- anunciar sucesso para tecnologia assistiva;
- oferecer desfazer apenas se existir operação de domínio realmente reversível.

---

## 10. Transações consolidadas

A página `/transacoes` contém uma única lista de todas as contas bancárias da
empresa. O filtro de conta oferece recortes sem duplicar telas ou templates.

Filtros adicionais:

- conciliada/não conciliada;
- escrita API/OFX;
- faixa de valor;
- ID Pluggy;
- ID Bling.

O detalhe é somente leitura, exceto quando o estado permite uma ação explícita.
Exportação, se adicionada, respeita exatamente os filtros atuais e o tenant da
sessão.

---

## 11. Lotes OFX

Lista por conta bancária e período, com:

- data de geração;
- conta;
- quantidade de transações;
- valor total de créditos e débitos;
- checksum/status;
- download;
- confirmação de upload no Bling.

Baixar não equivale a conciliar. “Confirmar upload” é uma ação separada e exige
confirmação clara.

---

## 12. Perfil

O perfil mostra:

- empresa;
- responsável;
- e-mail de acesso;
- alteração de senha;
- encerramento de sessões;
- logout.

Troca de empresa não existe no v1. O frontend não apresenta seletor de tenant.

---

## 13. Componentes compartilhados

Componentes Thymeleaf reutilizáveis:

- app shell;
- sidebar e navegação mobile;
- cabeçalho de página;
- alertas e toasts;
- badge de estado;
- indicador de confiança;
- filtro de conta bancária;
- paginação;
- empty state;
- skeleton/loading;
- modal de confirmação;
- painel lateral;
- resumo de integração;
- fragmento de erro.

Componentes de negócio recebem view models prontos. Templates não calculam regra
de classificação, taxa, permissão ou transição de estado.

---

## 14. Contrato HTMX

- Páginas completas são retornadas para navegação comum.
- Requisições HTMX retornam somente o fragmento solicitado.
- Todo POST inclui CSRF.
- Botões exibem estado ocupado e impedem envio duplo.
- Filtros GET usam debounce apenas na busca textual.
- `hx-push-url` mantém a navegação do histórico coerente.
- Respostas 422 substituem o formulário com erros.
- Respostas 409 recarregam o item e informam que ele foi alterado.
- Recurso ausente ou de outra empresa usa a mesma resposta 404 genérica.
- Sessão expirada dispara redirecionamento para login.
- Polling é usado somente em sincronizações em andamento e para ao chegar a um
  estado terminal.

Fragments devem possuir um elemento raiz estável e IDs previsíveis. O backend não
retorna JavaScript arbitrário para executar no navegador.

---

## 15. Rotas e fragments principais

| Método | Rota | View/fragment |
|---|---|---|
| `GET` | `/entrar` | `auth/login` |
| `GET` | `/cadastro` | `auth/cadastro` |
| `POST` | `/cadastro` | redirect onboarding ou formulário 422 |
| `GET` | `/onboarding` | `onboarding/index` |
| `POST` | `/integracoes/pluggy/conectar` | sessão de conexão/fragmento |
| `GET` | `/integracoes/status` | `integracoes/status` |
| `GET` | `/inicio` | `inicio/index` |
| `GET` | `/inicio/resumo` | `inicio/resumo` |
| `GET` | `/revisao` | `revisao/index` |
| `GET` | `/revisao/fila` | `revisao/fila` |
| `GET` | `/revisao/{id}` | `revisao/detalhe` |
| `POST` | `/revisao/{id}/aprovar` | linha + resumo atualizados |
| `POST` | `/revisao/{id}/classificar` | detalhe atualizado ou 422 |
| `POST` | `/revisao/{id}/match` | detalhe atualizado ou 422 |
| `POST` | `/revisao/{id}/ofx` | linha + resumo atualizados |
| `GET` | `/transacoes` | `transacoes/index` |
| `GET` | `/transacoes/lista` | `transacoes/lista` |
| `GET` | `/integracoes` | `integracoes/index` |
| `GET` | `/ofx/lotes` | `ofx/index` |
| `POST` | `/ofx/lotes/{id}/confirmar` | lote atualizado |
| `GET` | `/perfil` | `perfil/index` |

---

## 16. Responsividade

Breakpoints seguem os tokens do Bootstrap, sem lógica de servidor baseada em
user-agent.

- Desktop: sidebar, tabela e painel lateral simultâneos quando houver espaço.
- Tablet: sidebar recolhível e painel sobreposto.
- Mobile: navegação inferior, cards e detalhe em tela cheia.
- Alvos de toque têm no mínimo 44 × 44 px.
- Ações críticas permanecem acessíveis sem hover.
- Filtros mobile abrem em painel próprio e mostram a quantidade ativa.

---

## 17. Linguagem visual

- Interface clara e de alta densidade moderada no desktop.
- Verde da marca como destaque, não como única indicação de sucesso.
- Vermelho reservado para falha ou ação destrutiva.
- Valores monetários usam numerais tabulares quando disponíveis.
- Crédito e débito sempre combinam cor, ícone e texto.
- Confiança é exibida como “Alta”, “Média” ou “Baixa”, acompanhada do percentual
  no detalhe.
- Estados usam vocabulário de negócio, evitando nomes técnicos de enum.

Paleta final, logo e tipografia dependem dos ativos oficiais da Plante Uma Flor.
Até lá, o tema deve ser implementado por CSS custom properties substituíveis.

---

## 18. Acessibilidade

- HTML semântico antes de ARIA.
- Todos os campos possuem `label` visível.
- Foco retorna para local coerente após atualizações HTMX.
- Modais e painéis prendem foco e devolvem ao acionador ao fechar.
- Mensagens assíncronas usam região `aria-live` adequada.
- Contraste mínimo WCAG AA.
- Estado nunca depende somente de cor.
- Tabela possui cabeçalhos associados; cards preservam rótulos dos valores.
- Fluxos principais são operáveis somente por teclado.

---

## 19. Segurança no frontend

- O navegador nunca escolhe nem envia `empresa_id` como autoridade.
- IDs de recursos podem aparecer na rota, mas o backend sempre consulta por ID e
  empresa da sessão.
- Dados de outra empresa resultam em 404 genérico.
- 401 representa ausência de autenticação; 403, permissão insuficiente dentro do
  próprio tenant.
- CSRF permanece habilitado em todas as mutações.
- Descrição, documento e dados bancários são escapados pelo Thymeleaf.
- Tokens Pluggy/Bling não são armazenados em `localStorage`, `sessionStorage` ou
  HTML.
- Informações sensíveis são mascaradas nas listas e reveladas apenas quando
  necessárias no detalhe.
- Logout usa POST com CSRF.

---

## 20. Estrutura Java, templates e assets

### 20.1 Java por feature

O frontend server-side não forma um pacote global `controller`. Cada tela fica no
subpacote `web` da feature correspondente e depende de contratos em `application`
ou `query`.

```text
revisao
├── application
│   └── RevisarTransacao.java
├── query
│   ├── ConsultarFilaRevisao.java
│   └── RevisaoQueryService.java
└── web
    ├── RevisaoController.java
    ├── RevisaoForms.java
    └── RevisaoViewModels.java
```

- Controllers são classes concretas e não estendem uma classe base.
- Forms e view models pertencem a `web` e usam `record` quando imutáveis.
- Controllers não recebem entidades JPA nem acessam Spring Data.
- Interfaces de caso de uso ficam em `application`; consultas de tela ficam em
  `query`.
- Templates são as Views do MVC e permanecem em `resources/templates`.

### 20.2 Templates e assets

```text
src/main/resources
├── templates
│   ├── layout
│   │   ├── app.html
│   │   └── auth.html
│   ├── fragments
│   │   ├── navegacao.html
│   │   ├── feedback.html
│   │   ├── filtros.html
│   │   └── paginacao.html
│   ├── auth
│   ├── onboarding
│   ├── inicio
│   ├── revisao
│   ├── transacoes
│   ├── integracoes
│   ├── ofx
│   └── perfil
└── static
    ├── css
    │   ├── tokens.css
    │   ├── app.css
    │   └── components.css
    ├── js
    │   ├── app.js
    │   └── pluggy-connect.js
    └── img
```

Evitar um único template gigante. Fragments são divididos por responsabilidade,
sem granularidade de um arquivo por pequeno elemento visual.

---

## 21. Estados obrigatórios de tela

Toda página de dados contempla:

- carregando;
- vazia pela primeira vez;
- vazia pelos filtros;
- sucesso;
- erro recuperável;
- erro sem permissão;
- sessão expirada;
- dado alterado concorrentemente;
- integração desconectada.

Empty states devem dizer por que não há dados e oferecer a próxima ação correta.

---

## 22. Testes

### 22.1 MVC e templates

- página completa versus fragmento HTMX;
- filtros preservados na query string;
- paginação e ordenação;
- validação 422;
- conflito 409;
- redirect de sessão expirada;
- CSRF em todos os POSTs;
- 404 para recurso de outra empresa.

### 22.2 Fluxos de navegador

Cobrir com testes end-to-end os caminhos críticos:

1. cadastro → onboarding;
2. conexão Pluggy simulada → primeira sincronização;
3. login → revisão → aprovação;
4. alteração de classificação com erro e correção;
5. geração e confirmação de OFX;
6. tentativa de acesso cruzado entre duas empresas;
7. uso mobile nos fluxos principais.

Clients externos são simulados; a suíte não depende de Pluggy ou Bling reais.

---

## 23. Critérios de aceite

- Cadastro cria sessão e inicia onboarding.
- Usuário só visualiza dados da própria empresa.
- A tentativa de acessar ID de outro tenant retorna 404 genérico.
- Pluggy é conectado após o cadastro e fica associado à empresa autenticada.
- Todas as contas bancárias aparecem em uma lista consolidada com filtro.
- Aprovar uma transação não recarrega a página inteira.
- Filtros e paginação permanecem após abrir e fechar um detalhe.
- Desktop usa tabela; celular usa cards sem rolagem horizontal obrigatória.
- Todos os POSTs HTMX carregam CSRF e impedem envio duplo.
- Sessão expirada redireciona corretamente para login.
- Componentes principais funcionam por teclado e não dependem somente de cor.
- O frontend não contém segredo ou token de integração persistido no navegador.

---

## 24. Decisões pendentes

1. Cadastro público ou somente por convite/administração.
2. Verificação de e-mail e recuperação de senha.
3. Logo, paleta e tipografia oficiais.
4. Se o Bling é obrigatório no onboarding ou pode ser conectado depois.
5. Dados exatos pedidos no cadastro da empresa, incluindo CNPJ.
6. Política para desconectar uma integração com eventos pendentes.
7. Contrato final do componente de conexão Pluggy.
