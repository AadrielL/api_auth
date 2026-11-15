# Auditoria de Localidade, Vínculo Protegido e Algoritmo Anti-Panelinha (Frente 03)

Este documento descreve a arquitetura oficial de isolamento regional (`tenant_id`), proteção da carteira de clientes por link de indicação (`?ref=`) e a fórmula auditável de recomendação **Anti-Panelinha** (sem impulsionamento pago).

---

## 1. Mapeamento Multi-Tenant e Privacidade Regional (M3-01 e M3-02)

- **Preenchimento de CEP com Cache Local (`localStorage`):**
  - Quando o usuário digita os 8 dígitos do CEP em `RegisterPage.tsx`, o cliente consulta primeiro o cache local (`mygo_cep_cache_<cep>`).
  - Apenas se o CEP não estiver em cache é feita a chamada ao ViaCEP, economizando banda e evitando chamadas repetidas.
- **Privacidade do Profissional:**
  - Os endpoints públicos (`/auth/parceiros-destaque`) expõem apenas **Bairro/Região**, **Cidade** e **Estado (UF)**, jamais o CPF, telefone pessoal ou endereço residencial completo.

---

## 2. Regra de Ouro do Vínculo Protegido (`?ref=codigo_eletricista` — M3-03)

- **Captura do Parâmetro `?ref=`:**
  - Qualquer acesso com `?ref=<id_ou_tenant>` em `/onboarding`, `/register` ou `/parceiros` salva `ref_eletricista_vinculado` no `localStorage`.
- **Bloqueio de Concorrentes:**
  - Se `ref_eletricista_vinculado` estiver presente, a tela de **Parceiros** exibe exclusivamente o eletricista do convite (ou encaminha diretamente para o orçamento/cadastro vinculado a ele), garantindo que o cliente convidado por um profissional **jamais veja concorrentes**.
- **Link Compartilhável do Eletricista:**
  - No Painel Principal (`MainViewPage.tsx`), o eletricista possui um card dedicado **"Meu Link Exclusivo de Indicação (`?ref=`)"** com botão de cópia rápida e envio via WhatsApp.

---

## 3. Fórmula Auditável de Recomendação Anti-Panelinha (M3-04)

Quando o cliente chega de forma **orgânica (sem `?ref=`)**, o ranking ordena os profissionais pelo **Índice Anti-Panelinha (`scoreRecomendacao`)**, composto por três fatores transparentes e **zero influência de planos pagos**:

$$\text{Score Total} = \text{Score Proximidade} + \text{Score Avaliação} + \text{Bônus Novato}$$

| Fator | Peso / Cálculo | Regra de Negócio |
| :--- | :--- | :--- |
| **1. Proximidade Geográfica** | `+40 pts` (Mesmo Bairro)<br>`+25 pts` (Mesma Cidade)<br>`+10 pts` (Mesmo Estado/UF) | Prioriza profissionais que atendem a mesma região do cliente sem usar GPS pesado em tempo real. |
| **2. Média de Avaliação Real** | $\text{Nota Média} \times 10$ (máx. `50 pts`) | Baseada exclusivamente nas avaliações pós-serviço (`1 a 5 estrelas`) de orçamentos com status `ACEITO`. |
| **3. Bônus de Novato (Anti-Panelinha)** | `+25 pts` (se `< 5` avaliações/serviços)<br>`+12 pts` (se `5 a 10` serviços) | Garante que eletricistas recém-cadastrados com boa postura apareçam no topo junto aos veteranos bem avaliados. |

---

## 4. Avaliação Pós-Serviço Vinculada ao Aceite (M3-05)

- **Prevenção contra Avaliações Falsas:**
  - O formulário de avaliação rápida (`1 a 5 estrelas` + tags objetivas: `Pontual`, `Limpo`, `Preço Justo`, `Atendimento Técnico NBR 5410`) só é liberado na página da proposta (`/proposta/:id`) **após** o orçamento estar com status `ACEITO`.
- **Atualização da Nota Média:**
  - Ao enviar a avaliação (`POST /auth/avaliar-profissional`), a API recalcula a média ponderada do eletricista (`avaliacao` e `totalAvaliacoes`) em tempo real.
