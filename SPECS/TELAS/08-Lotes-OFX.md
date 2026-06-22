# Tela 08 — Lotes OFX

> **Rota autenticada:** `GET /ofx/lotes`  
> **Template:** `templates/ofx/index.html`  
> **Web:** `ofx.web`
> **Casos de uso:** `ofx.application`
> **Consultas:** `ofx.query`
> **Domínio/persistência:** `ofx.domain`, `ofx.persistence`

## 1. Função

Agrupar transações roteadas para OFX, gerar arquivo por conta bancária, permitir
download e registrar a confirmação humana de upload no Bling.

Download e conciliação são estados diferentes. Baixar um arquivo nunca marca o
lote como conciliado.

## 2. Interfaces de aplicação

`ConsultarLotesOfx` fica em `ofx.query`. Comandos e acesso ao arquivo ficam em
`ofx.application`; `GeradorOfx` e invariantes do lote ficam em `ofx.domain`.

```java
public interface ConsultarLotesOfx {
    Pagina<LoteOfxItemView> consultar(
        EmpresaId empresaId,
        FiltroLoteOfx filtro,
        Paginacao paginacao
    );

    LoteOfxDetalheView detalhar(EmpresaId empresaId, UUID loteId);
}

public interface GerenciarLotesOfx {
    LoteOfxGerado gerar(EmpresaId empresaId, GerarLoteOfxCommand comando);
    void confirmarUpload(EmpresaId empresaId, ConfirmarUploadOfxCommand comando);
}

public interface ObterArquivoOfx {
    ArquivoOfx obter(EmpresaId empresaId, UUID loteId);
}
```

`ArquivoOfx` contém nome seguro, media type, tamanho/checksum e fonte de bytes
streamável. Não expõe caminho local.

## 3. Controllers

```java
@Controller
@RequestMapping("/ofx/lotes")
final class LoteOfxController {
    private final ConsultarLotesOfx consultas;
    private final GerenciarLotesOfx comandos;
    private final ObterArquivoOfx arquivos;
}
```

Rotas:

| Método | Rota | Função |
|---|---|---|
| GET | `/ofx/lotes` | página/lista |
| GET | `/ofx/lotes/lista` | fragmento filtrado |
| GET | `/ofx/lotes/{id}` | detalhe |
| POST | `/ofx/lotes` | gerar lote |
| GET | `/ofx/lotes/{id}/arquivo` | download |
| POST | `/ofx/lotes/{id}/confirmar` | confirmar upload |

Controller não estende classe base, não monta OFX e não acessa filesystem
diretamente.

## 4. Commands e view models

```java
public record GerarLoteOfxCommand(
    UUID contaBancariaId,
    LocalDate inicio,
    LocalDate fim,
    Set<UUID> transacoesSelecionadas
) {}

public record ConfirmarUploadOfxCommand(
    UUID loteId,
    long version,
    String observacao
) {}
```

O command não inclui empresa, nome de arquivo, checksum fornecido pelo cliente ou
estado final arbitrário.

`LoteOfxItemView` apresenta conta, período, quantidade, totais de crédito/débito,
status, data de geração e ações permitidas.

## 5. Composição de templates

- `ofx/index.html` compõe `layout/app.html`.
- `ofx/filtros.html` contém conta, período e status.
- `ofx/lista.html` contém tabela/cards.
- `ofx/detalhe.html` contém itens e checksum.
- `ofx/form-gerar.html` cria lote.
- `ofx/modal-confirmar.html` confirma upload.

Composição substitui herança. Geração do conteúdo pertence ao `GeradorOfx`, não
ao template.

## 6. Encapsulamento

- Todas as consultas usam `empresaId` da sessão.
- Conta e transações selecionadas são revalidadas no mesmo tenant.
- Apenas transações em estado elegível entram no lote.
- Um item não entra em dois lotes ativos.
- Arquivo é imutável após geração e possui checksum.
- Nome é produzido no servidor com caracteres seguros.
- Download não aceita path ou nome fornecido pelo usuário.
- Confirmação muda estados pelo agregado/caso de uso e grava auditoria.

## 7. Geração

1. Usuário escolhe conta e período.
2. Backend mostra itens elegíveis e totais.
3. POST envia seleção e CSRF.
4. Caso de uso bloqueia/revalida transações.
5. `GeradorOfx` cria conteúdo determinístico.
6. Lote, itens e checksum são persistidos atomicamente.
7. Transações passam para `EM_LOTE_OFX`.
8. Resposta mostra o lote pronto para download.

Lotes são sempre de uma única conta bancária.

## 8. Download

- Resposta usa media type de OFX apropriado.
- `Content-Disposition` usa nome sanitizado.
- Conteúdo pode ser streamado sem carregar arquivo grande inteiro na memória.
- Download é auditável, mas não altera o estado de conciliação.
- Lote de outro tenant retorna 404.

## 9. Confirmação de upload

Antes de confirmar, a tela mostra:

- arquivo/lote;
- conta bancária;
- quantidade e totais;
- aviso de que a confirmação encerra os itens no sistema.

Após confirmação:

- lote recebe estado `UPLOAD_CONFIRMADO`;
- transações relacionadas chegam a `CONCILIADO`;
- usuário e timestamp entram na auditoria;
- operação repetida é idempotente.

## 10. Estados

```text
GERANDO
DISPONIVEL
FALHA_GERACAO
UPLOAD_CONFIRMADO
CANCELADO
```

Cancelamento só é permitido antes da confirmação e quando não viola transições
das transações. Se não for necessário no v1, não apresentar a ação.

## 11. HTMX e erros

- Filtros GET atualizam lista e URL.
- Gerar atualiza lista, detalhe e contadores.
- 422 mostra itens que deixaram de ser elegíveis.
- 409 recarrega lote/version atual.
- 404 cobre inexistente e outro tenant.
- Download é navegação HTTP normal, não swap HTMX.
- Confirmação possui fallback sem JavaScript.

## 12. Responsividade e acessibilidade

- Desktop: tabela com totais alinhados.
- Mobile: cards e detalhe em tela cheia.
- Modal de confirmação gerencia foco.
- Créditos e débitos têm rótulos além da cor.
- Download usa link/botão com nome e tamanho do arquivo.
- Checksum longo permite quebra visual sem perder conteúdo.

## 13. Testes

- gera somente com conta e transações do mesmo tenant;
- rejeita item de outra empresa com 404/validação segura;
- impede dois lotes ativos para a mesma transação;
- arquivo é determinístico e checksum confere;
- download não muda conciliação;
- nome do arquivo não permite path traversal;
- confirmação muda lote e transações atomicamente;
- confirmação repetida é idempotente;
- conflito de versão retorna 409;
- mobile e fallback sem HTMX funcionam.

## 14. Critérios de aceite

- Lote contém uma única conta bancária e apenas itens elegíveis.
- Usuário consegue gerar, baixar e confirmar sem confundir as etapas.
- Download nunca equivale a conciliação.
- Arquivo e confirmação são isolados por empresa e auditáveis.
- Controller não conhece a implementação do formato OFX.
