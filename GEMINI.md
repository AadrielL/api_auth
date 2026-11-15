# Regras Oficiais do Projeto — Antigravity & Gemini Pro (Ecossistema MyGo)

## 🚨 REGRA DE OURO DE VERSIONAMENTO E COMMIT ATÔMICO (OBRIGATÓRIA)
- **Commit por Classe/Arquivo Alterado:** É estritamente obrigatório realizar `git add <arquivo>` e `git commit -m "<tipo>(<escopo>): <descrição>"` **imediatamente a cada classe, componente ou arquivo modificado**.
- **Proibido Acumular Modificações:** Nunca agrupe diversas classes ou funcionalidades distintas em um único commit monolítico. Cada classe ou ajuste concluído deve gerar seu próprio commit atômico.
- **Sincronização Contínua (`git pull` + `commit`):** Antes de iniciar qualquer alteração ou abrir PR, execute `git pull` da branch base para manter o histórico atualizado e sem conflitos.
- **Branches e Pull Requests:**
  - Nunca faça push direto na `main` ou `master`.
  - Utilize branches padronizadas (`feat/frente01-<tarefa>`, `feat/frente02-<tarefa>`, `feat/frente03-<tarefa>` ou `fix/...`).
  - Preencha sempre `.github/pull_request_template.md` listando os commits atômicos por classe.

---

## 1. Regras de Localidade, Vínculo e Ranking (Frente 03)
- Todo registro de cliente, profissional e pedido deve respeitar o isolamento por `tenant_id` (cidade/profissional).
- Nunca crie lógica de "impulsionamento pago" ou destaque por assinatura no ranking de busca.
- Respeite rigorosamente o vínculo de indicação (`?ref=`) quando o cliente vier pelo link de um eletricista (nunca exibir concorrentes).
- Proteja a privacidade: exiba bairro/raio de atendimento, nunca o endereço residencial completo, CPF ou telefone pessoal do profissional.
- Sempre que alterar a fórmula de pontuação anti-panelinha ou os campos de localidade do multi-tenant, mantenha atualizado `docs/LOCALIDADE_E_RANKING.md`.

## 2. Regras de Negócio — Orçamentos e Materiais (Frente 02)
- Todo orçamento pertence a um tenant (cidade) e a um `profissional_id` específico.
- Nunca hardcode o telefone da distribuidora diretamente nos componentes visuais; leia da configuração do tenant (padrão inicial: Capital Elétrica - Palmas).
- Sempre registre o evento de intenção de cotação no banco antes de redirecionar para o WhatsApp (`/api/materiais/cotacao-log`).
- Respeite limites de segurança da VPS básica sem comprometer o fluxo gratuito principal (teto diário anti-abuso: 15 orçamentos/dia no plano FREE).

## 3. Qualidade de Código, Mobile-First & Economia de VPS (Frente 01)
- Arquitetura Dual: Angular na porta `4200` (PC/Desktop) e React + `@ionic/react` na porta `4201` (Mobile Nativo).
- Valide valores numéricos (preços e quantidades >= 0) no frontend e no backend.
- Evite consultas pesadas de geolocalização em tempo real; priorize índice por cidade/bairro e cacheie respostas de CEP no cliente (`localStorage`) para não repetir chamadas externas.
- Mantenha compatibilidade total com telas mobile (360px+), botões e inputs com `min-height: 44px`, `inputMode` numérico/decimal, proteção `isSubmitting` contra clique duplo e `debounce` de 400ms em buscas.
