# API Authentication Service Documentation

## Overview
This authentication service provides secure access to API endpoints using JSON Web Tokens (JWT). It supports various authentication strategies and is designed for multitenancy.

## Authentication Flow
1. **User Registration**: Users can register with their credentials.
2. **Token Generation**: After successful login, a JWT token is generated that expires after a defined time.
3. **Accessing API**: Users send the JWT token in the Authorization header when making API requests.

## JWT Tokens
- JWT tokens contain three parts: Header, Payload, and Signature.
- **Header**: Contains metadata, typically the token type and signing algorithm.
- **Payload**: Contains claims, which are statements about an entity (usually the user) and additional data.
- **Signature**: This is used to verify that the sender of the JWT is who it says it is and to ensure that the message wasn't changed along the way.

## Multitenancy Strategy
- The service is built to support multiple tenants (clients) within a single instance of the application.
- Each tenant's data is isolated, ensuring data privacy and integrity.
- Configuration settings allow per-tenant customization of authentication rules and policies.

## ⚙️ Variáveis de Ambiente (Environment Variables)

A aplicação lê configurações sensíveis exclusivamente por variáveis de ambiente.

| Variável | Obrigatória? | Descrição | Exemplo Local |
|---|---|---|---|
| `JWT_SECRET` | **Sim** | Segredo para assinatura e validação dos tokens JWT (mínimo 256 bits). **Deve ser idêntico em todas as APIs**. | `sua_chave_secreta_jwt_deve_ter_pelo_menos_32_caracteres` |
| `DB_URL` | Não | JDBC URL do PostgreSQL (default: `jdbc:postgresql://localhost:5432/postgres`) | `jdbc:postgresql://localhost:5432/postgres` |
| `DB_USERNAME` | Não | Usuário do PostgreSQL (default: `postgres`) | `postgres` |
| `DB_PASSWORD` | Não | Senha do PostgreSQL (default: `1234`) | `1234` |
| `JPA_DDL` | Não | Estratégia de DDL do Hibernate (default: `update`) | `update` ou `validate` (em prod) |
| `MP_ACCESS_TOKEN` | Não | Access Token da API do Mercado Pago para processar pagamentos | `APP_USR-...` |

---

### 💻 Executando em Ambiente Local

1. **Via PowerShell (Windows):**
   ```powershell
   $env:JWT_SECRET="sua_chave_secreta_jwt_deve_ter_pelo_menos_32_caracteres"
   ./mvnw.cmd spring-boot:run
   ```

2. **Via Bash (Linux / Mac):**
   ```bash
   export JWT_SECRET="sua_chave_secreta_jwt_deve_ter_pelo_menos_32_caracteres"
   ./mvnw spring-boot:run
   ```

3. **Via IntelliJ IDEA / VS Code:**
   Copie o arquivo `.env.example` para `.env` ou defina em `Run/Debug Configurations -> Environment Variables`:
   ```
   JWT_SECRET=sua_chave_secreta_jwt_deve_ter_pelo_menos_32_caracteres
   ```

---

### 🚀 Configuração em Produção (Render / Railway / Docker)

Defina as variáveis no painel da plataforma de deploy (aba *Environment Variables*):
- `JWT_SECRET`: *[Defina uma chave criptográfica forte aleatória]*
- `DB_URL`: `jdbc:postgresql://<host>:<port>/<database>`
- `DB_USERNAME`: `<usuario>`
- `DB_PASSWORD`: `<senha>`
- `JPA_DDL`: `update`
- `MP_ACCESS_TOKEN`: `APP_USR-...`

---

## Conclusion
This service provides a robust foundation for implementing secure authentication across a variety of applications. Proper implementation of JWT and an effective multitenancy strategy ensure that the service is scalable and secure.