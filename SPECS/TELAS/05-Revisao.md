# Tela 05 — Fila de revisão

> **Rota autenticada:** `GET /revisao`  
> **Template:** `templates/revisao/index.html`  
> **Pacotes:** `revisao`, `transacao`, `classificacao`, `match`, `outbox`, `ofx`

## 1. Função

Resolver transações ambíguas com o mínimo de passos e sem permitir transições
inválidas. É a tela central do produto e deve suportar cerca de 20 revisões por
dia com uso prioritário em desktop.

## 2. Interfaces de aplicação

Leitura:

```java
public interface ConsultarFilaRevisao {
    Pagina<FilaRevisaoItemView> consultar(
        EmpresaId empresaId,
        FiltroRevisao filtro,
        Paginacao paginacao
    );

    RevisaoDetalheView detalhar(EmpresaId empresaId, UUID transacaoId);
    ResumoRevisaoView resumir(EmpresaId empresaId, FiltroRevisao filtro);
}
```

Comandos:

```java
public interface RevisarTransacao {
    RevisaoResultado aprovar(EmpresaId empresaId, AprovarCommand comando);
    RevisaoResultado classificar(EmpresaId empresaId, ClassificarCommand comando);
    RevisaoResultado selecionarMatch(EmpresaId empresaId, SelecionarMatchCommand comando);
    RevisaoResultado rotearParaOfx(EmpresaId empresaId, RotearOfxCommand comando);
    RevisaoResultado solicitarRetry(EmpresaId empresaId, SolicitarRetryCommand comando);
}
```

`RevisaoQueryService` implementa consultas com projeções. `RevisaoService`
implementa comandos e coordena o agregado `Transacao`, outbox e OFX. A interface
não expõe entidade JPA.

## 3. Commands

Todos os commands são records e incluem a versão otimista da transação:

```java
public record AprovarCommand(UUID transacaoId, long version) {}

public record ClassificarCommand(
    UUID transacaoId,
    long version,
    ClasseTransacao classe,
    String justificativa
) {}

public record SelecionarMatchCommand(
    UUID transacaoId,
    long version,
    String matchTipo,
    String matchId
) {}
```

Nenhum command contém `empresaId`, estado de destino arbitrário, taxa calculada
no navegador ou ID de outbox.

## 4. Controller

```java
@Controller
@RequestMapping("/revisao")
final class RevisaoController {
    private final ConsultarFilaRevisao consultas;
    private final RevisarTransacao comandos;
}
```

Rotas:

| Método | Rota | Responsabilidade |
|---|---|---|
| GET | `/revisao` | página completa |
| GET | `/revisao/fila` | lista filtrada |
| GET | `/revisao/resumo` | contadores dos filtros |
| GET | `/revisao/{id}` | painel de detalhe |
| POST | `/revisao/{id}/aprovar` | aprovar sugestão atual |
| POST | `/revisao/{id}/classificar` | corrigir classificação |
| POST | `/revisao/{id}/match` | escolher candidato |
| POST | `/revisao/{id}/ofx` | rotear para OFX |
| POST | `/revisao/{id}/retry` | reenfileirar falha elegível |

Controller não estende classe base, não muda entidades e não calcula taxa.

## 5. View models

```java
public record FilaRevisaoItemView(
    UUID id,
    long version,
    LocalDate data,
    ContaResumoView conta,
    String descricao,
    Direcao direcao,
    BigDecimal valorLiquido,
    String classeSugerida,
    ConfiancaView confianca,
    String motivo,
    StatusVisual estado
) {}
```

`RevisaoDetalheView` acrescenta dados brutos seguros, candidatos de match,
cálculo bruto/líquido/taxa, auditoria e `AcoesPermitidasView`.

A view renderiza somente ações explicitamente permitidas pelo backend. Ocultar o
botão não substitui a validação do caso de uso.

## 6. Composição de templates

- `revisao/index.html` compõe `layout/app.html`.
- `revisao/filtros.html` contém filtros.
- `revisao/resumo.html` contém totais.
- `revisao/fila.html` escolhe tabela ou cards por CSS responsivo.
- `revisao/linha.html` representa um item desktop.
- `revisao/card.html` representa um item mobile.
- `revisao/detalhe.html` representa painel lateral/tela cheia.
- `revisao/form-classificacao.html` e `form-match.html` são fragments próprios.

Templates não estendem templates nem classes Java. O layout é composto por
fragments; regras ficam nos view models e casos de uso.

## 7. Encapsulamento

- `empresaId` vem da sessão.
- Consulta usa `id + empresa_id`; item de outro tenant responde 404.
- Controller não recebe entidade por data binding.
- Estado muda somente por método do agregado `Transacao`.
- Candidatos válidos são recalculados/validados no servidor ao confirmar.
- `version` detecta revisão concorrente.
- Aprovação que segue para API cria estado + outbox na mesma transação local.
- Erro de integração não expõe payload ou credencial.
- Auditoria registra usuário, ação, valores anteriores e novos.

## 8. Filtros

`FiltroRevisao` aceita apenas valores conhecidos:

- texto;
- data inicial/final;
- IDs de contas bancárias pertencentes à empresa;
- direção;
- classe;
- faixa de confiança;
- motivo;
- estado.

Ordenação usa allowlist. Paginação possui limite máximo. Filtros inválidos
retornam 422 ou são normalizados de modo explícito; nunca viram SQL dinâmico.

## 9. Fluxos de ação

### 9.1 Aprovar

1. Envia ID, version e CSRF.
2. Backend recarrega por ID + empresa.
3. Agregado valida estado e sugestão atual.
4. Caso de uso aplica rota API ou OFX adequada.
5. Auditoria e outbox são gravados atomicamente quando aplicável.
6. Resposta atualiza linha, painel e contadores.

### 9.2 Corrigir classificação

1. Usuário escolhe classe e informa justificativa quando exigida.
2. Backend valida opções permitidas.
3. Match anterior incompatível é removido pelo agregado.
4. Novos candidatos podem ser buscados fora da transação de escrita.
5. Item permanece ou sai da fila conforme resultado.

### 9.3 Selecionar match

O navegador envia apenas a referência do candidato. Backend confirma que ele
pertence à mesma empresa, ainda está aberto no Bling e continua compatível.

### 9.4 Retry

Disponível apenas em `FALHA` recuperável. Cria/reagenda evento de outbox de forma
idempotente; não faz POST direto no Bling durante a requisição.

## 10. HTMX

- Filtros GET atualizam fila e usam `hx-push-url`.
- Abrir item carrega painel por GET.
- Mutações usam POST e CSRF.
- Resposta de sucesso contém item principal e fragments `hx-swap-oob` para resumo.
- Botão fica desabilitado durante requisição.
- 422 substitui apenas o formulário inválido.
- 409 recarrega o detalhe atual e informa alteração concorrente.
- 404 fecha o painel e apresenta mensagem genérica.
- Após ação, foco vai ao próximo item ou à confirmação.

## 11. HTTP e erros

| Situação | Status |
|---|---|
| não autenticado | redirect login/401 HTMX tratado |
| item da própria empresa inexistente | 404 |
| item de outra empresa | 404 idêntico |
| command inválido | 422 |
| transição proibida | 422 |
| versão desatualizada | 409 |
| sucesso | 200 fragment ou 303 em fallback sem HTMX |

Todas as ações devem funcionar sem HTMX por progressive enhancement, ainda que
com recarregamento completo.

## 12. Responsividade

- Desktop: tabela + painel lateral.
- Tablet: tabela reduzida + painel sobreposto.
- Mobile: cards + detalhe em tela cheia.
- Barra de ação do detalhe permanece acessível no mobile.
- Não exigir rolagem horizontal para operar.

## 13. Acessibilidade

- Tabela com cabeçalhos associados.
- Cards mantêm rótulos dos dados.
- Painel gerencia foco e fecha com Escape.
- Confiança combina texto e percentual.
- Crédito/débito combina texto, ícone e cor.
- Resultado da ação é anunciado em região `aria-live`.
- Confirmações destrutivas não dependem de toast temporário.

## 14. Testes

- lista e resumo são isolados por empresa;
- filtro de conta rejeita conta de outro tenant;
- cada command consulta por ID + empresa;
- item cruzado retorna 404;
- aprovação válida muda agregado e cria outbox atomicamente;
- falha na outbox faz rollback da mudança;
- match é revalidado;
- conflito de version retorna 409;
- ação inválida retorna 422;
- fragmentos HTMX atualizam fila e resumo;
- fallback sem HTMX funciona;
- keyboard flow e layout mobile são cobertos no E2E.

## 15. Critérios de aceite

- Revisar um item exige no máximo abrir, conferir e confirmar.
- Nenhuma ação escreve no Bling dentro da requisição web.
- Transições e tenant são validados no backend.
- Filtros permanecem após revisar um item.
- Usuário consegue processar a fila no desktop e no celular.

