# Acao Games

Sistema web para loja de videogames, com backend em Spring Boot e frontend em React/Vite.

## Estrutura

```text
backend/     API Java + Spring Boot
frontend/    Aplicacao React + Vite
scripts/     Atalhos PowerShell para iniciar backend e frontend
```

## Banco de dados

- SGBD: SQLite, embarcado no backend
- Arquivo local: `data/acaogames.db`
- Schema: `backend/src/main/resources/schema.sql`
- A estrutura e os dados iniciais sao criados automaticamente na primeira execucao.
- Nao e necessario instalar PostgreSQL, pgAdmin ou qualquer servidor de banco.

Para fazer backup, feche o sistema e copie o arquivo `data/acaogames.db`. Os arquivos auxiliares
`acaogames.db-wal` e `acaogames.db-shm`, quando existirem, tambem devem ser copiados se o sistema
nao puder ser encerrado antes do backup.

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

- Stack: Java 8 + Spring Boot 2.7 + SQLite
- Porta padrao: `8080`
- Maven Wrapper incluido: use `backend/mvnw.cmd` no Windows ou `backend/mvnw` em Unix/macOS.

### Requisitos

Use um JDK, nao apenas JRE. No Windows, o wrapper tenta usar `C:\Program Files\Java\jdk1.8.0_111` quando existir. Em outros ambientes, configure `JAVA_HOME` para um JDK compativel.

### Variaveis

```text
SPRING_DATASOURCE_URL=jdbc:sqlite:../data/acaogames.db?busy_timeout=5000&foreign_keys=on&journal_mode=WAL
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
APP_DEFAULT_USER_ID=1
```

`SPRING_DATASOURCE_URL` e opcional. Sem essa variavel, o sistema usa automaticamente
`data/acaogames.db` na raiz do projeto.

### Como rodar

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Ou use:

```powershell
.\scripts\start-backend.ps1
```

O atalho usa primeiro o JAR pronto, que consome menos recursos na inicializacao. Para gerar ou
atualizar esse arquivo depois de alterar o backend, execute `backend\mvnw.cmd package`.

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
- Proxy local: `/api` para `http://localhost:8080`

### Como rodar

```powershell
cd frontend
npm install
npm run build
npm run preview
```

Ou use:

```powershell
.\scripts\start-frontend.ps1
```

O script inicia o build compilado, reduzindo o trabalho do navegador e do Node no PC da loja. Se a pasta `dist` ainda nao existir, o build e criado automaticamente uma unica vez.

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
