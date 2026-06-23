# Correções de implementação por tela

> Lacunas entre o que foi implementado (branch `dev`, passos 1–3) e o contrato de
> cada tela em [`SPECS/TELAS`](../TELAS). Cada documento lista to-dos acionáveis,
> com prioridade, arquivo-alvo e teste a adicionar.

Origem: auditoria de 2026-06-23 cruzando o código com as telas 01–04.

| Tela | Cobertura atual | Documento |
|---|---|---|
| 01 Login | 🔧 Agora concluído (35 testes verdes) | — |
| 02 Cadastro | 🔧 Agora concluído (35 testes verdes) | — |
| 03 Onboarding | ~25% (fake) | [OnboardingCorreção.md](./OnboardingCorreção.md) |
| 04 Início | ~5% (placeholder) | [InicioCorreção.md](./InicioCorreção.md) |

Os itens 💅 Hardening e 🧱 Estrutural de Login/Cadastro (rate limit, cookie
`Secure` em produção, mostrar/ocultar senha, layout `auth.html`, unicidade de
CNPJ, `char[] senha`, `aria-describedby`, aceite de termos) ficaram pendentes
de propósito — ver Premissas em [`PROXIMOS-PASSOS.md`](../PROXIMOS-PASSOS.md).

## Convenção de prioridade

- 🔧 **Agora** — desvio real de contrato, barato de corrigir.
- ⏳ **Depois** — depende de outro passo (ex.: `transacao`) ou serviço externo (Pluggy/Bling real).
- 💅 **Hardening** — segurança/UX que não bloqueia o fluxo.

Marque `- [x]` ao concluir e remova o item do "pendente" do topo do documento.
