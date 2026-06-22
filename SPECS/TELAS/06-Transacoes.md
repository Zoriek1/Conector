# Tela 06 — Transações

> **Rota autenticada:** `GET /transacoes`  
> **Template:** `templates/transacoes/index.html`  
> **Web:** `transacao.web`
> **Consultas:** `transacao.query`
> **Domínio/persistência:** `transacao.domain`, `transacao.persistence`

## 1. Função

Consultar o histórico consolidado de todas as contas bancárias da empresa. A
tela é prioritariamente de leitura e auditoria; mudanças operacionais continuam
na fila de revisão.

Não existe uma tabela física ou uma página por conta bancária. O filtro de conta
recorta a mesma lista consolidada.

## 2. Interface de leitura

Interface, filtros de consulta, projeções e implementação ficam em
`transacao.query`. Controller e view models de apresentação ficam em
`transacao.web`.

```java
public interface ConsultarTransacoes {
    Pagina<TransacaoListaItemView> consultar(
        EmpresaId empresaId,
        FiltroTransacao filtro,
        Paginacao paginacao
    );

    TransacaoDetalheView detalhar(EmpresaId empresaId, UUID transacaoId);
    OpcoesFiltroTransacaoView opcoes(EmpresaId empresaId);
}
```

`TransacaoQueryService` implementa a interface com projeções. Ele não retorna
`Transacao` JPA e não reutiliza o repositório de escrita como API genérica.

## 3. Controller

```java
@Controller
@RequestMapping("/transacoes")
final class TransacaoController {
    private final ConsultarTransacoes consultas;

    // GET /transacoes
    // GET /transacoes/lista
    // GET /transacoes/{id}
}
```

O controller não estende classe base, não acessa JPA e não contém regra de
conciliação.

## 4. Filtros

```java
public record FiltroTransacao(
    String busca,
    LocalDate inicio,
    LocalDate fim,
    Set<UUID> contas,
    Set<Direcao> direcoes,
    Set<ClasseTransacao> classes,
    Set<EstadoTransacao> estados,
    BigDecimal valorMinimo,
    BigDecimal valorMaximo,
    String pluggyId,
    String blingId
) {}
```

`empresaId` não faz parte do filtro web. IDs de conta são validados contra a
empresa autenticada. Período, tamanho de página e ordenação possuem limites.

## 5. View models

`TransacaoListaItemView` contém somente dados da tabela/card. O detalhe acrescenta:

- descrição original;
- documento mascarado;
- E2E ID quando permitido;
- classificação e justificativa;
- match Bling;
- bruto, líquido e taxa;
- par de transferência;
- IDs técnicos úteis para suporte;
- histórico de estados e auditoria;
- lote OFX/outbox relacionado sem payload sensível.

Nenhum view model expõe token, secret ou entidade persistente.

## 6. Composição de templates

- `transacoes/index.html` compõe `layout/app.html`.
- `transacoes/filtros.html` contém busca e filtros.
- `transacoes/lista.html` contém tabela/cards.
- `transacoes/linha.html` e `card.html` apresentam um item.
- `transacoes/detalhe.html` abre painel de leitura.
- `fragments/paginacao.html` é compartilhado.

Não há herança de controller ou template. O app shell é composto com fragments.

## 7. Encapsulamento

- Toda query recebe empresa da sessão.
- Detalhe consulta por `id + empresa_id`.
- Item de outro tenant retorna 404 sem indicar existência.
- Lista usa projeção imutável; não expõe setters ou lazy loading na view.
- Parâmetros de ordenação passam por allowlist.
- Documento é mascarado antes de chegar ao template.
- Cálculos financeiros são produzidos no backend com `BigDecimal`.

## 8. Lista desktop

Colunas:

- data;
- conta bancária;
- descrição;
- direção;
- valor líquido;
- classe;
- estado;
- rota API/OFX;
- ação `Ver detalhes`.

Ordenação padrão: data decrescente, depois criação decrescente. Valor fica
alinhado à direita e formatado em BRL.

## 9. Mobile

Cards mostram valor, direção, data, conta, descrição, classe e estado. Filtros
abrem em painel e indicam quantos estão ativos. Detalhe ocupa a tela.

## 10. HTMX e navegação

- Filtros usam GET e atualizam URL.
- Busca textual usa debounce moderado.
- Paginação preserva filtros.
- Abrir detalhe não perde posição da lista.
- Voltar do navegador restaura a consulta.
- Resposta HTMX contém apenas lista/detalhe; navegação comum recebe layout.

Esta tela não faz polling contínuo. Um botão `Atualizar` pode refazer a consulta.

## 11. Estados

| Estado | Interface |
|---|---|
| nenhuma transação | convite para verificar integrações |
| nenhum resultado | limpar/ajustar filtros |
| integração sincronizando | aviso não bloqueante |
| detalhe removido/inacessível | 404 genérico e fecha painel |
| consulta falhou | tentar novamente + correlation ID |

## 12. Exportação

Exportação não faz parte do v1 inicial. Quando adicionada:

- será caso de uso próprio;
- repetirá os filtros atuais no servidor;
- aplicará tenant da sessão;
- terá limite ou processamento assíncrono;
- não será implementada montando CSV no controller.

## 13. Acessibilidade

- Cabeçalhos e ordenação têm nomes acessíveis.
- Estado de ordenação usa `aria-sort`.
- Valores não dependem só de cor.
- Cards mobile preservam rótulos.
- Foco vai ao painel ao abrir e retorna à linha ao fechar.
- Paginação usa links reais.

## 14. Testes

- lista consolidada reúne contas da mesma empresa;
- filtro por conta restringe sem criar outra tabela;
- conta de outra empresa não produz dados;
- detalhe cruzado retorna 404;
- busca, intervalo monetário e datas funcionam;
- ordenação rejeita campo não permitido;
- limite de página é aplicado;
- projeção mascara documento;
- HTMX e fallback de página completa funcionam;
- filtros sobrevivem à paginação e ao voltar.

## 15. Critérios de aceite

- Usuário consulta todas as contas em uma única lista.
- Filtros por conta, período, classe e estado podem ser combinados.
- Nenhuma consulta ou detalhe atravessa tenant.
- A tela é somente leitura e não contorna a fila de revisão.
- Desktop e mobile apresentam os mesmos dados essenciais.
