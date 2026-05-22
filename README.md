# Ação Games

Nova estrutura do projeto separada em backend e frontend web.

## Estrutura

```text
backend/        API em Java + Spring Boot
frontend/       Aplicação web em React + HTML + CSS
```

## Status da migração

A estrutura antiga em JavaFX foi removida do repositório.
O projeto agora segue oficialmente com:

- backend web em Spring Boot
- frontend web em React

## Backend

- Stack: Java 8 + Spring Boot 2.7 + PostgreSQL
- Porta padrão: `8080`
- Banco padrão: `jdbc:postgresql://localhost:5432/acaogames`
- Credenciais padrão:
  - usuário: `postgres`
  - senha: `1234`

### Endpoints principais

- `GET /api/dashboard`
- `GET|POST|PUT|DELETE /api/clientes`
- `GET|POST|PUT|DELETE /api/produtos`
- `GET|POST|PUT|DELETE /api/equipamentos`
- `GET|POST|PUT|DELETE /api/ordens-servico`
- `GET /api/ordens-servico/status`

### Observação sobre a versão

Esta base foi montada com `Spring Boot 2.7.x` porque o ambiente atual está em `Java 8`.
Se vocês quiserem migrar para `Spring Boot 3`, o próximo passo é atualizar o JDK para `17+`.

### Como subir

```bash
cd backend
mvn spring-boot:run
```

O arquivo [application.yml](backend/src/main/resources/application.yml) aceita sobrescrita por variáveis de ambiente:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_DEFAULT_USER_ID`

O script [schema.sql](backend/src/main/resources/schema.sql) cria a base mínima necessária se as tabelas ainda não existirem.

## Frontend

- Stack: React + Vite + CSS
- Porta padrão: `5173`
- URL padrão da API: `http://localhost:8080/api`
- Templates visuais disponíveis:
  - `Balcao Pro`: claro, compacto e focado em atendimento
  - `Bancada Tecnica`: escuro, técnico e orientado a ordens de serviço
  - `Arcade Neon`: mais expressivo para identidade gamer

### Como subir

```bash
cd frontend
npm install
npm run dev
```

Se quiser apontar para outra API, copie `frontend/.env.example` para `.env` e ajuste:

```bash
VITE_API_URL=http://localhost:8080/api
```

## Próximos passos naturais

- adicionar autenticação e perfis de usuário
- criar módulos de vendas, caixa e orçamento
- separar DTOs de resposta para deixar a API mais estável
- adicionar testes automatizados no backend e no frontend
