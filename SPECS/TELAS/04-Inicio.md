# Tela 04 — Início

> **Rota autenticada:** `GET /inicio`  
> **Template:** `templates/inicio/index.html`  
> **Pacote principal:** `com.planteumaflor.conciliador.inicio`

## 1. Função

Apresentar a situação operacional da empresa e direcionar o usuário ao próximo
trabalho. A tela responde:

- as integrações estão funcionando?;
- existem itens aguardando revisão?;
- há falhas de escrita?;
- qual foi a atividade recente?;
- quando ocorreu a última sincronização?

Não é uma ferramenta de BI nem substitui a fila de revisão.

## 2. Interface de leitura

```java
public interface ConsultarInicio {
    InicioView consultar(EmpresaId empresaId, PeriodoResumo periodo);
}

public record InicioView(
    ResumoOperacionalView resumo,
    List<IntegracaoResumoView> integracoes,
    List<AtividadeRecenteView> atividades,
    Instant atualizadoEm
) {}
```

`InicioQueryService` implementa `ConsultarInicio`. É um serviço de leitura e pode
usar projeções SQL/JPA otimizadas; não carrega agregados apenas para contar
registros.

## 3. Controller

```java
@Controller
@RequestMapping("/inicio")
final class InicioController {
    private final ConsultarInicio consultarInicio;

    // GET /inicio          -> página completa
    // GET /inicio/resumo  -> fragments atualizáveis
}
```

O controller não estende classe base e não acessa repositórios. `EmpresaId` vem
do `UsuarioPrincipal` por um resolvedor autenticado.

## 4. View models

```java
public record ResumoOperacionalView(
    long emRevisao,
    long falhas,
    long aguardandoApi,
    long aguardandoOfx,
    long conciliadasNoPeriodo
) {}

public record IntegracaoResumoView(
    String nome,
    StatusVisual status,
    String mensagem,
    Instant ultimaSincronizacao,
    URI acao
) {}
```

View models já chegam com texto de apresentação seguro. A view não traduz enums
de infraestrutura nem decide se um estado é crítico.

## 5. Composição de templates

- `inicio/index.html` compõe `layout/app.html`.
- `inicio/resumo.html` contém cards de contagem.
- `inicio/integracoes.html` contém status Pluggy/Bling.
- `inicio/atividade.html` contém atividade recente.
- `fragments/empty-state.html` é usado quando ainda não houve ingestão.

Não há herança. Cards recebem dados por composição e fragments.

## 6. Encapsulamento

- Queries exigem `EmpresaId` como argumento do servidor.
- Contagens são calculadas no banco e nunca em listas carregadas na memória.
- Links de cards usam filtros conhecidos, sem transportar `empresa_id`.
- A tela não altera estado financeiro.
- Falhas externas aparecem sanitizadas; stack traces e payloads não chegam à UI.
- Atividade contém apenas registros da empresa autenticada.

## 7. Conteúdo e ações

Cards principais:

| Card | Clique |
|---|---|
| Em revisão | `/revisao?estado=EM_REVISAO` |
| Falhas | `/revisao?estado=FALHA` |
| Aguardando API | `/transacoes?estado=AGUARDANDO_ESCRITA_API` |
| Aguardando OFX | `/ofx/lotes` ou filtro correspondente |
| Conciliadas | `/transacoes?estado=CONCILIADO` |

Ações adicionais:

- sincronizar agora, se permitido;
- reconectar integração com problema;
- continuar onboarding incompleto.

`Sincronizar agora` chama caso de uso próprio; não faz parte de
`ConsultarInicio`.

## 8. Atualização HTMX

- A página completa é carregada uma vez.
- Após uma sincronização manual, `/inicio/resumo` atualiza cards e integrações.
- Não usar polling permanente quando tudo está estável.
- Durante sincronização ativa, polling moderado para ao terminar.
- O timestamp “Atualizado em” informa a idade dos dados.

## 9. Estados

| Estado | Exibição |
|---|---|
| empresa sem Pluggy | empty state com `Conectar bancos` |
| sincronizando | progresso e última etapa conhecida |
| operação normal | cards + atividade |
| integração requer atenção | banner acionável |
| sem pendências | mensagem positiva sem esconder histórico |
| falha de consulta | erro recuperável e correlation ID |

## 10. Responsividade

- Desktop: grade de cards, integrações e atividade em duas colunas.
- Tablet: cards em duas colunas.
- Mobile: cards empilhados e atividade em lista.
- Valores importantes aparecem antes de gráficos; o v1 não precisa de gráficos.

## 11. Acessibilidade

- Contadores têm rótulo completo.
- Cards clicáveis usam links reais.
- Status combina texto e ícone.
- Atualização assíncrona usa `aria-live="polite"` sem anunciar toda a página.
- Ordem visual e ordem do DOM são iguais.

## 12. Testes

- todos os totais são filtrados por empresa;
- empresas com dados iguais recebem contagens independentes;
- links produzem filtros válidos;
- integração com falha mostra ação adequada;
- página vazia direciona ao onboarding;
- fragmento HTMX não devolve layout completo;
- polling termina em estado terminal;
- query não carrega entidades completas.

## 13. Critérios de aceite

- Usuário identifica pendências e falhas sem abrir outras telas.
- Nenhuma métrica mistura empresas.
- Todo card acionável leva à lista já filtrada.
- A página permanece útil no celular sem gráficos ou tabelas largas.

