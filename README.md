# 🔐 API Auth - Serviço de Identidade e Multitenancy (Porta 8083)

Este microserviço é o guardião do ecossistema. Ele é responsável por autenticar usuários, gerenciar permissões (RBAC) e injetar o contexto de **Multitenancy** através de tokens JWT.

---

## 🛠️ Tecnologias e Conceitos Aplicados

* **Java 21 & Spring Boot 3**
* **Spring Security**: Configuração de segurança Stateless.
* **JWT (JSON Web Token)**: Utilização da biblioteca `auth0` para geração e validação de tokens.
* **BCrypt**: Hashing de senhas para armazenamento seguro no banco.
* **CORS Configuration**: Configurado especificamente para aceitar requisições de aplicações **Javascript (Angular)** rodando em `localhost:4200`.
* **Multitenancy Strategy**: Injeção do `tenant_id` dentro do payload do Token, permitindo o isolamento de dados nas APIs de cálculo e orçamento.

---

## 🚀 Endpoints de Autenticação

| Método | Endpoint | Acesso | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Público | Autentica e retorna o JWT com Nome, Email, Role e TenantId. |
| `POST` | `/auth/register` | Público | Registra um novo profissional (Default: ADMIN). |
| `GET` | `/auth/me` | Autenticado | Retorna os dados do perfil do usuário logado via Contexto de Segurança. |

---

## 🔐 Estrutura do Token JWT

O token gerado por esta API não apenas autentica o usuário, mas carrega informações vitais para o funcionamento dos outros microserviços:

```json
{
  "iss": "auth-api",
  "sub": "eletricista@email.com",
  "role": "ADMIN",
  "tenant_id": "uuid-do-profissional",
  "exp": 1711215600
}   