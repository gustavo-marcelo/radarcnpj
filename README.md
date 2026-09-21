# Radar CNPJ

Consulta e análise de dados públicos do Cadastro Nacional da Pessoa Jurídica (CNPJ), baseados nos arquivos de **[Dados Abertos da Receita Federal](https://www.gov.br/receitafederal/dados/cnpj-metadados.pdf)**.

Aplicação completa em monorepo: **API REST** (Spring Boot + PostgreSQL) e **frontend** (Angular + Tailwind CSS), com segurança via JWT, importação sob demanda dos arquivos da Receita e exportação de consultas para **XLSX**.

## Stack

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Spring Security (JWT), Spring JDBC, Flyway, OpenAPI (springdoc) |
| Banco de dados | PostgreSQL (dados de produção: ~70M empresas, ~68M estabelecimentos) |
| Exportação | Apache POI 5.4 (`poi-ooxml`) |
| Frontend | Angular 22, Tailwind CSS v4, TypeScript, RxJS |
| Qualidade | JUnit 5 (integração), Jasmine + Karma, ESLint, Prettier, Commitlint, Husky |

## Estrutura do repositório

```
├── cnpj/            # API REST (backend)
│   └── src/
│       ├── main/java/ia/espalha/cnpj/
│       │   ├── auth/          # autenticação e JWT
│       │   ├── consulta/      # consultas (empresas, estabelecimentos, domínios) e exportação XLSX
│       │   ├── importacao/    # importação dos arquivos da Receita Federal
│       │   ├── usuario/       # gestão de usuários
│       │   ├── config/        # segurança e OpenAPI
│       │   └── resources/db/migration/   # migrações Flyway (schema + índices)
│   ├── frontend/   # SPA Angular (interface web)
│   ├── dados/      # arquivos .zip da Receita Federal (gitignored)
│   └── docs/       # diretrizes de projeto e metadados
```

## Funcionalidades

### Backend (`/api`)

| Módulo | Endpoints | Descrição |
| --- | --- | --- |
| Autenticação | `POST /api/auth/login`, `POST /api/auth/registrar`, `GET /api/auth/me` | Login JWT, cadastro público (opcional) e dados do usuário autenticado |
| Empresas | `GET /api/empresas`, `GET /api/empresas/{cnpjBasico}` | Lista paginada (filtros: razão social, CNPJ básico, natureza jurídica) e detalhe |
| Estabelecimentos | `GET /api/estabelecimentos`, `GET /api/estabelecimentos/{cnpjBasico}/{ordem}/{dv}` | Lista paginada (filtros: razão social, nome fantasia, CNPJ, UF, município, CNAE, situação, matriz/filial) e detalhe |
| Domínios | `GET /api/dominios/{cnaes,municipios,naturezas,paises,qualificacoes,motivos}` | Tabelas de referência usadas em listas e filtros |
| Exportação | `GET /api/exportacao/empresas`, `GET /api/exportacao/estabelecimentos`, `GET /api/exportacao/cnaes` | Gera **XLSX** em streaming (arquivo temporário + `Content-Length`) respeitando os filtros e o parâmetro `limite` |
| Importação | `GET /api/importacao/status`, `POST /api/importacao/iniciar`, `POST /api/importacao/cancelar` | Importação (re)importada dos arquivos da Receita |
| Configuração | `GET/PUT /api/config/importacao` | Diretório de dados, threads e política de truncamento |

Documentação interativa em `/swagger-ui.html` (OpenAPI em `/v3/api-docs`).

### Frontend (SPA)

Telas: **Login** e **Cadastro**, **Empresas** (lista + detalhe), **Estabelecimentos** (lista + detalhe), **Consulta CNAE** (com atalho para os estabelecimentos do código) e telas de **administração** (importação e usuários, acesso `ADMIN`). Qualquer consulta listada pode ser exportada para **XLSX** com limite definido pelo usuário.

## Como executar

### Docker Compose (recomendado)

Com **apenas o Docker instalado**, suba tudo (banco + API + frontend):

```bash
cd docker
cp .env.example .env   # opcional — ajuste segredos/admin/CORS
docker compose up -d --build
```

- Frontend: http://localhost:4200
- API: http://localhost:8080 (Swagger em http://localhost:8080/swagger-ui.html)
- Admin padrão: `admin@exemplo.com` / `admin123`

Observações:

- Tudo o que é Docker (compose, Dockerfiles, nginx, `.env`) vive em `docker/`.
- Os Dockerfiles **não compilam do código**: baixam o **último Release** do GitHub
  (`releases/latest`) — o jar da API e o zip do SPA. Em fork, ajuste `GITHUB_REPO` no `.env`.
  **Atenção:** é preciso existir ao menos um Release antes do `--build`.
- Cole os arquivos `.zip` da Receita em `dados/` (na raiz, montado em `/dados` no contêiner da API) e
  dispare a importação pela tela de administração ou com `APP_IMPORT_ENABLED=true`.
- Os dados ficam em um volume Docker (`cnpj-db-data`); ele só é criado na primeira execução.
- Logs: `docker compose logs -f api`.

### Releases (GitHub)

Os artefatos publicados em um Release são consumidos pelo Docker:

- `cnpj.jar` — API Spring Boot (backend)
- `radar-cnpj-frontend.zip` — SPA Angular (frontend, `apiBaseUrl=/api`)

Dois jeitos de criar um Release (workflow `Release` em `.github/workflows/release.yml`):

```bash
# 1) Automação via tag
git tag v0.1.0       # ou a versão que desejar
git push origin v0.1.0
```

ou

```text
2) GitHub → Actions → Release → "Run workflow" (preencha a versão, sem o "v").
```

O workflow gera as notas do release e publica os dois assets.

### Pré-requisitos (execução manual)

- [JDK 25](https://openjdk.org/projects/jdk/25/) (o backend usa o **Maven Wrapper** incluso — sem instalação de Maven)
- [Node.js 20+](https://nodejs.org/) e Angular CLI
- [PostgreSQL](https://www.postgresql.org/)

### 1. Banco de dados

```sql
CREATE USER cnpj WITH PASSWORD 'cnpj';
CREATE DATABASE cnpj OWNER cnpj;
```

O schema e os índices são criados pela **Flyway** (`baseline-on-migrate`, ideal para bases já populadas).

### 2. Backend

As configurações vivem em `cnpj/src/main/resources/application.properties` e podem ser sobrescritas por variáveis de ambiente (relaxed binding), ex.:

| Propriedade | Variável de ambiente | Padrão |
| --- | --- | --- |
| Conexão | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | `jdbc:postgresql://localhost:5432/cnpj` |
| Segredo do JWT | `APP_JWT_SECRET` | chave de desenvolvimento (troque em produção) |
| Expiração do token | `APP_JWT_EXPIRATION_SECONDS` | `86400` |
| Origens CORS | `APP_CORS_ORIGENS` | `http://localhost:4200` (dev) |
| Cadastro público | `APP_CADASTRO_PUBLICO` | `false` |
| Diretório de dados | `APP_DADOS_DIR` | `../dados` |
| Importação no boot | `APP_IMPORT_ENABLED` | `false` |

```bash
cd cnpj
./mvnw -B clean spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Usuário administrador padrão em desenvolvimento: `admin@exemplo.com` / `admin123`.

Testes:

```bash
./mvnw -B clean test    # 25 testes de integração
```

### 3. Frontend

Em `frontend/src/environments/environment.ts`, ajuste `apiBaseUrl` (padrão de dev: `https://api-dev.espalha.ai/api`). Em desenvolvimento o Angular pode usar o proxy (`proxy.conf.json`) para evitar problemas de CORS:

```bash
cd frontend
npm install
npm start               # http://localhost:4200
npm run build          # build de produção (usa environment.prod.ts)
npm run lint
npm run test:ci
```

### 4. Dados da Receita Federal (importação)

Baixe os arquivos de [dados abertos do CNPJ](https://dados-abertos-rf-cnpj.casadosdados.com.br/) e coloque os `.zip` em `dados/` (padrão, configurável). A importação pode ser feita pela tela de administração ou habilitando `APP_IMPORT_ENABLED=true` para executar no boot — com suporte a múltiplas threads e carregamento por streaming (sem estourar memória em bases com dezenas de milhões de registros).

*Nota: cada arquivo Compressed=N gera múltiplos volumes (`Empresas0.zip`, `Empresas1.zip`, ...) que são processados um a um — extração para diretório temporário, importação e limpeza automática.*

## Consultas

- Empresas e estabelecimentos são pesquisáveis por termos parciais (`ILIKE`) apoiados por índices `pg_trgm`.
- A descrição de domínios (município, natureza jurídica, etc.) é resolvida **no momento da consulta**, via joins/lookup em lote, e exibida junto ao código (ex.: `6493 — IBITINGA/SP`).
- A busca por CNAE secundária usa índice `GIN` sobre o array de códigos.

## Convenções de desenvolvimento

- **Commits**: Conventional Commits (Commitlint) + Husky + lint-staged.
- **Frontend**: ESLint (angular-eslint), Prettier, componentes standalone com `@if`/`@for` e `ChangeDetectionStrategy.OnPush`.
- **Backend**: testes de integração com execução em porta aleatória (`@SpringBootTest(webEnvironment = RANDOM_PORT)`).

## Roadmap

- [ ] CI/CD (GitHub Actions: testes e lint em todo push)
- [x] Release automatizado (workflow publica jar + SPA no GitHub; Docker baixa o último release)
- [ ] Autenticação por refresh token
- [ ] Exportação assíncrona com tarefa em segundo plano e download posterior
- [x] Docker Compose (postgres + api + frontend)
- [ ] Página pública de exemplos de API

## Licença

A definir. Este projeto consome dados públicos do **Receita Federal do Brasil** — consulte os [metadados e termos de uso](https://www.gov.br/receitafederal/dados/cnpj-metadados.pdf).

## Me pague um café

Gostou do projeto e quer apoiar? Um cafezinho é muito bem-vindo! ☕

Minha chave PIX:

```
46f6223e-682a-4898-be28-3cbb42d80d6f
```

Obrigado por usar o **Radar CNPJ**!