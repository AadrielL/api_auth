# Fluxo Git, Pull Request e Regra de Commit por Classe Alterada — MyGo

## 1. Regra Obrigatória: Commit Atômico a Cada Classe/Arquivo Modificado

Para garantir rastreabilidade total entre a equipe e facilitar code reviews e rollbacks:

1. **Uma Classe / Componente Modificado = Um Commit Imediato:**
   - Alterou uma entidade (`User.java`, `Orcamento.java`)? Faça o commit imediatamente antes de editar a próxima classe.
   - Alterou um Service ou Controller (`UserService.java`, `AuthController.java`)? Faça o commit dedicado daquela classe.
   - Alterou uma tela ou serviço no Frontend (`RegisterPage.tsx`, `HistoricoPage.tsx`)? Faça o commit específico daquele arquivo.

2. **Sincronização (`git pull` antes de codar e antes do PR):**
   ```bash
   git pull origin main --rebase
   ```

3. **Padrão de Nomenclatura de Branches:**
   - `feat/frente01-<nome-curto>` (Mobile-First & Economia de VPS)
   - `feat/frente02-<nome-curto>` (Calculadora, Aceite 1-Clique e Materiais)
   - `feat/frente03-<nome-curto>` (Localidade Multi-Tenant, Vínculo `?ref=` e Anti-Panelinha)
   - `fix/frente0X-<nome-curto>` (Correções de bugs)

4. **Padrão Conventional Commits por Classe:**
   - `feat(User.java): add CPF, phone SMS verification and geolocation fields`
   - `feat(UserService.java): implement geolocation matching and post-service rating`
   - `feat(RegisterPage.tsx): add CEP cache and SMS/Email verification toggle`

5. **Validação Obrigatória Antes do Push / Pull Request:**
   - **Frontends (`front_mygo_react` / `orcamento-front`):** Executar `npm run build` com zero erros.
   - **Backends (`api_1`, `api-calculadora`, `api-materiais`):** Verificar compilação limpa e isolamento de `tenantId`.
   - Preencher integralmente o `.github/pull_request_template.md`.
