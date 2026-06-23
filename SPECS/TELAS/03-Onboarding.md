# Tela 03 — Onboarding

> **Rota autenticada:** `GET /onboarding`  
> **Template:** `templates/onboarding/index.html`  
> **Web:** `onboarding.web`, `pluggy.web`, `bling.web`
> **Casos de uso:** `onboarding.application`, `pluggy.application`,
> `bling.application`, `ingest.application`
> **Clients externos:** `pluggy.client`, `bling.client`

## 1. Função

Levar uma empresa recém-cadastrada até o primeiro ciclo útil: conectar Pluggy,
confirmar as contas encontradas, conectar Bling e iniciar a primeira
sincronização.

O onboarding é retomável. Recarregar ou sair não perde o progresso.

## 2. Etapas

```text
CONTA_CRIADA
PLUGGY_PENDENTE
CONTAS_PENDENTES
BLING_PENDENTE
SINCRONIZANDO
CONCLUIDO
REQUER_ATENCAO
```

O estado é derivado das integrações persistidas; não é controlado por um número
de etapa enviado pelo navegador. `PLUGGY_PENDENTE` cobre tanto informar as
credenciais do Meu Pluggy da empresa quanto concluir a conexão dos bancos pelo
widget.

## 3. Interfaces de aplicação

As interfaces de coordenação da tela ficam em `onboarding.application`.
Operações específicas dos fornecedores permanecem nas respectivas features,
atrás de `pluggy.application` e `bling.application`.

```java
public interface ConsultarOnboarding {
    OnboardingView consultar(EmpresaId empresaId);
}

public interface IniciarConexaoPluggy {
    ConexaoPluggyIniciada executar(EmpresaId empresaId);
}

public interface ConfirmarContasPluggy {
    void executar(EmpresaId empresaId, ConfirmarContasCommand comando);
}

public interface IniciarConexaoBling {
    URI executar(EmpresaId empresaId);
}

public interface IniciarPrimeiraSincronizacao {
    SincronizacaoId executar(EmpresaId empresaId);
}
```

Os controllers recebem `EmpresaId` exclusivamente do principal autenticado.

## 4. View models

```java
public record OnboardingView(
    EtapaOnboarding etapa,
    IntegracaoView pluggy,
    List<ContaDescobertaView> contas,
    IntegracaoView bling,
    SincronizacaoView sincronizacao,
    boolean podeConcluir
) {}
```

Todos são records de leitura. Tokens, refresh tokens e secrets não fazem parte
dos view models.

## 5. Controllers

```text
OnboardingController
├── GET  /onboarding
├── GET  /onboarding/status
└── POST /onboarding/sincronizar

PluggyController
├── POST /integracoes/pluggy/conectar
├── POST /integracoes/pluggy/confirmar-contas
└── endpoint de retorno conforme contrato Pluggy

BlingOAuthController
├── POST /integracoes/bling/conectar
└── GET  /integracoes/bling/retorno
```

Nenhum controller estende classe base. Cada um depende das interfaces de caso de
uso correspondentes.

## 6. Composição de templates

- `onboarding/index.html` compõe `layout/app.html` em modo reduzido.
- `onboarding/etapas.html` mostra progresso.
- `onboarding/pluggy.html` contém o card Pluggy.
- `onboarding/contas.html` lista contas descobertas.
- `onboarding/bling.html` contém conexão OAuth.
- `onboarding/sincronizacao.html` mostra progresso e resultado.

Fragments são substituídos por HTMX. Não existe herança de telas.

## 7. Encapsulamento

- As credenciais do **Meu Pluggy são por empresa**: informadas no onboarding e
  guardadas no banco, criptografadas. Só as credenciais do app **Bling** (OAuth)
  vêm da configuração do servidor.
- Identificadores e tokens da empresa ficam no banco.
- O navegador recebe apenas token efêmero estritamente necessário ao componente
  de conexão Pluggy; as credenciais do Meu Pluggy nunca chegam ao navegador.
- `empresaId` nunca aparece como campo editável.
- Callback externo é correlacionado com estado assinado e de uso único.
- O estado apresentado é recalculado no servidor após cada ação.
- O frontend não marca uma integração como ativa por conta própria.

## 8. Fluxo Pluggy

1. Usuário informa as credenciais do seu **Meu Pluggy** (clientId/clientSecret);
   o backend valida e persiste por empresa, criptografadas.
2. Usuário escolhe `Conectar bancos`.
3. POST autenticado inicia conexão para a empresa da sessão **usando as
   credenciais do Meu Pluggy daquela empresa**.
4. Backend devolve fragmento com dados efêmeros necessários ao widget.
5. JavaScript local abre o componente Pluggy.
6. Conclusão é validada pelo backend.
7. IDs da integração e contas são persistidos com `empresa_id`.
8. Tela mostra contas descobertas.
9. Usuário confirma quais contas participarão do ingest.

O contrato exato do widget deve ser isolado em `PluggyConnectAdapter`; mudanças do
fornecedor não alteram o controller ou o modelo de onboarding.

## 9. Fluxo Bling

1. POST autenticado solicita conexão.
2. Backend cria `state` assinado, vinculado à empresa e sessão.
3. Navegador é redirecionado ao consentimento Bling.
4. Callback valida `state` e troca o código no backend.
5. Tokens são persistidos por empresa.
6. Redirect retorna ao onboarding com estado atualizado.

## 10. Primeira sincronização

- Inicia em background.
- Polling HTMX consulta `/onboarding/status` em intervalo moderado.
- Polling para em sucesso ou falha terminal.
- Usuário pode navegar e retornar.
- Conclusão mostra contas e quantidade importada.
- Botão final leva para `/inicio` ou diretamente à revisão quando houver itens.

## 11. Estados de erro

| Erro | Apresentação |
|---|---|
| usuário cancela Pluggy | card permanece pendente |
| callback inválido | mensagem genérica e nova tentativa |
| consentimento expirado | ação `Reconectar` |
| nenhuma conta encontrada | orientação para tentar outra instituição |
| Bling recusado | card pendente; ingest pode continuar |
| primeira sincronização falha | motivo sanitizado + tentar novamente |
| recurso de outro tenant | 404 genérico |

## 12. Segurança

- Toda mutação iniciada no app exige CSRF.
- Callbacks externos usam validação própria de assinatura/state.
- State é de uso único e possui expiração.
- Tokens não são enviados em query string interna, logs ou HTML persistente.
- Cada integração é consultada por ID e empresa da sessão.
- Repetir callback não duplica integração nem conta bancária.

## 13. Responsividade e acessibilidade

- Desktop usa stepper horizontal; mobile, lista vertical.
- Estado tem texto e ícone, nunca somente cor.
- Progresso assíncrono usa `aria-live="polite"`.
- Abertura do widget preserva retorno de foco.
- Botões de conexão informam quando uma janela externa será aberta.

## 14. Testes

- deriva etapa a partir de dados persistidos;
- retoma onboarding no ponto correto;
- vincula integração à empresa da sessão;
- não aceita empresa enviada pelo cliente;
- callback com state inválido/expirado falha;
- callback repetido é idempotente;
- duas empresas podem conectar contas sem mistura;
- polling para em estado terminal;
- tokens não aparecem no HTML ou logs de teste;
- primeira sincronização roda fora da requisição.

## 15. Critérios de aceite

- Empresa nova consegue conectar Pluggy sem editar `.env` próprio.
- Contas descobertas pertencem somente à empresa autenticada.
- Bling pode ser conectado e renovado por empresa.
- Onboarding sobrevive a logout, refresh e falha temporária.
- A tela não contém regra de integração; apenas chama interfaces de aplicação.
