# Correções de implementação por tela

> Lacunas entre o que foi implementado (branch `dev`, passos 1–3) e o contrato de
> cada tela em [`SPECS/TELAS`](../TELAS). Cada documento lista to-dos acionáveis,
> com prioridade, arquivo-alvo e teste a adicionar.

Origem: auditoria de 2026-06-23 cruzando o código com as telas 01–04.

| Tela | Cobertura atual | Documento |
|---|---|---|
| 01 Login | ✅ Concluído | — |
| 02 Cadastro | ✅ Concluído | — |
| 03 Onboarding | ~55% (conectores reais, contas, Cora; Bling OAuth e HTMX pendentes) | [OnboardingCorreção.md](./OnboardingCorreção.md) |
| 04 Início | ~70% (núcleo entregue; fragmento HTMX e "sincronizar agora" pendentes) | [InicioCorreção.md](./InicioCorreção.md) |
| 05 Revisão | ~40% (fila básica; comandos HTMX e detalhe pendentes) | — |
| 06 Transações | ~50% (listagem básica; filtros avançados pendentes) | — |
| 07 Integrações | ~70% (Cora + Pluggy por empresa; Bling pendente) | — |
| 08 Lotes OFX | ~50% (geração básica; confirmação `CONCILIADO` pendente) | — |
| 09 Perfil | ✅ Concluído (alterar dados, senha, encerrar outras sessões) | — |

Os itens 💅 Hardening e 🧱 Estrutural de Login/Cadastro (rate limit, cookie
`Secure` em produção, mostrar/ocultar senha, layout `auth.html`, unicidade de
CNPJ, `char[] senha`, `aria-describedby`, aceite de termos) ficaram pendentes
de propósito — ver Premissas em [`PROXIMOS-PASSOS.md`](../PROXIMOS-PASSOS.md).

## Convenção de prioridade

- 🔧 **Agora** — desvio real de contrato, barato de corrigir.
- ⏳ **Depois** — depende de outro passo (ex.: `transacao`) ou serviço externo (Pluggy/Bling real).
- 💅 **Hardening** — segurança/UX que não bloqueia o fluxo.

Marque `- [x]` ao concluir e remova o item do "pendente" do topo do documento.
