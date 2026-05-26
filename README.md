# Acao Games

Sistema web para loja de videogames, com backend em Spring Boot e frontend em React/Vite.

## Estrutura

```text
backend/     API Java + Spring Boot
frontend/    Aplicacao React + Vite
scripts/     Atalhos PowerShell para iniciar backend e frontend
```

## Banco de dados

- Banco esperado: `acaogames`
- SGBD: PostgreSQL
- Script oficial: `backend/src/main/resources/schema.sql`
- O script recria as tabelas com `DROP TABLE` e insere dados iniciais. Execute apenas dentro do banco `acaogames`.
- A inicializacao automatica do Spring esta desativada em `application.yml` com `spring.sql.init.mode: never`.

O schema inclui:

- clientes
- usuarios do sistema
- equipamentos
- produtos, incluindo foto do produto
- ordens de servico
- vendas e itens de venda
- caixa
- movimentacoes de estoque
- tabelas historicas de orcamento, garantia e itens de servico

## Backend

- Stack: Java 8 + Spring Boot 2.7 + PostgreSQL
- Porta padrao: `8080`
- Maven Wrapper incluido: use `backend/mvnw.cmd` no Windows ou `backend/mvnw` em Unix/macOS.

### Requisitos

Use um JDK, nao apenas JRE. No Windows, o wrapper tenta usar `C:\Program Files\Java\jdk1.8.0_111` quando existir. Em outros ambientes, configure `JAVA_HOME` para um JDK compativel.

### Variaveis

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/acaogames
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=1234
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
APP_DEFAULT_USER_ID=1
```

### Como rodar

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Ou use:

```powershell
.\scripts\start-backend.ps1
```

### Autenticacao

O endpoint publico de login e:

```text
POST /api/usuarios/login
```

As demais rotas `/api/**` exigem token Bearer:

```text
Authorization: Bearer <token>
```

O frontend salva o token da sessao e envia automaticamente nas chamadas HTTP. URLs de PDF e imagem tambem recebem o token via query string gerada pelo helper da API.

### Endpoints ativos principais

```text
GET /api/dashboard
GET|POST|PUT|DELETE /api/clientes
GET|POST|PUT|DELETE /api/produtos
GET|POST|PUT|DELETE /api/equipamentos
GET|POST|PUT|DELETE /api/ordens-servico
POST /api/ordens-servico/{id}/saida
GET /api/ordens-servico/{id}/pdf
GET|POST|PUT|DELETE /api/vendas
GET /api/vendas/{id}/pdf
GET|POST|PUT|DELETE /api/movimentos-caixa
GET /api/movimentos-caixa/saldo
GET /api/movimentos-caixa/saldo-hoje
GET /api/relatorios/caixa
GET /api/relatorios/vendas
GET /api/relatorios/ordens
GET /api/relatorios/estoque
GET /api/relatorios/clientes
```

Os relatorios aceitam:

```text
inicio=YYYY-MM-DD
fim=YYYY-MM-DD
busca=texto
page=0
size=20
```

## Frontend

- Stack: React 18 + Vite
- Porta padrao: `5173`
- Proxy de desenvolvimento: `/api` para `http://localhost:8080`

### Como rodar

```powershell
cd frontend
npm install
npm run dev
```

Ou use:

```powershell
.\scripts\start-frontend.ps1
```

## Validacao

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm run build
```

## Modulos removidos da API publica

Foram removidos endpoints sem tela/consumo atual:

- garantias
- orcamentos
- itens de servico
- itens de venda direto
- movimentacoes de estoque direto
- CRUD publico de usuarios

As tabelas historicas continuam no schema para preservar compatibilidade com o banco informado.
