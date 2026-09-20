# Plano de Implementação — API de Consulta CNPJ + Frontend

> Status: **implementado** (iteração atual: exportação XLSX e resolução de descrições de domínio).

## 1. Objetivo

Aplicação web para:

1. Autenticação (login com e-mail e senha) e cadastro/gestão de usuários.
2. Consultas de empresas e estabelecimentos (dados abertos da Receita Federal).
3. Configuração e disparo da importação dos dados do CNPJ.
4. Exportação das consultas para **XLSX**.

- Backend: projeto Spring Boot em `cnpj/` (originalmente CLI de importação, hoje API + importador no mesmo processo).
- Frontend: projeto Angular em `frontend/` (app `radar-cnpj`, Angular 22, standalone, Tailwind v4).

## 2. Arquitetura (vigente)

```
Navegador (Angular / radar-cnpj)
        |  HTTP + JWT (Bearer)
        v
API REST (Spring Boot - cnpj/)
        |  JDBC / JdbcTemplate + COPY (importação) + SXSSF (exportação XLSX)
        v
PostgreSQL (db cnpj, user cnpj)
```

- Monólito: API e importador rodam no mesmo processo Spring Boot.
- Banco único `cnpj` para dados de domínio, empresas, estabelecimentos, autenticação e configuração de importação.

## 3. Backend (`cnpj/`) — concluído

### 3.1 Dependências (`pom.xml`)

Adicionadas: `spring-boot-starter-web`, `-security`, `-validation`, `-actuator`, `-jdbc`, `-flyway` (+ `flyway-database-postgresql`),
`jjwt-api/impl/jackson` (0.12.6), `springdoc-openapi-starter-webmvc-ui` (3.1.1), **`poi-ooxml` (5.4.0)**.

### 3.2 Modelo de dados

- Tabelas de segurança via Flyway `V1`: `usuario`, `configuracao_importacao`, `importacao_execucao`.
- Admin inicial criado por seed (`app.admin.email`/`app.admin.senha`).
- Índices de consulta via Flyway `V2__indices_consulta.sql` + `V3__indices_texto.sql` (`pg_trgm`).
- CNAE secundário: **decisão tomada** — índice **GIN** sobre `string_to_array(...)` (sem tabela filha).
- `establish`: Flyway com `baseline-on-migrate` para base legada já populada.

### 3.3 Segurança e autenticação — concluído

- `BCryptPasswordEncoder`; JWT stateless (`Authorization: Bearer <token>`).
- Públicos: `POST /api/auth/login` e, se `app.cadastro-publico=true`, `POST /api/auth/registrar` (default `false`).
- Gestão de usuários e importação exigem `ADMIN`.
- CORS liberado para origens configuradas (`app.cors.origens`, inclui `https://front-dev.espalha.ai`).

## 3.4 Endpoints — vigentes (ver também o README)

| Método | Rota | Acesso |
|---|---|---|
| POST | `/api/auth/login` | público |
| POST | `/api/auth/registrar` | público (config.) |
| GET | `/api/auth/me` | autenticado |
| GET/POST/PUT/DELETE | `/api/usuarios` e `/api/usuarios/{id}` (+ `PUT /{id}/senha`) | ADMIN |
| GET/PUT | `/api/config/importacao` | ADMIN |
| POST | `/api/importacao/iniciar` (`202`/`409`) | ADMIN |
| GET | `/api/importacao/status` | ADMIN |
| POST | `/api/importacao/cancelar` | ADMIN |
| GET | `/api/empresas` (`razaoSocial`, `cnpjBasico`, `natureza`) | autenticado |
| GET | `/api/empresas/{cnpjBasico}` | autenticado |
| GET | `/api/estabelecimentos` (`cnpjBasico`, `nomeFantasia`, `cnae`+`tipoCnae`, `uf`, `municipio`, `situacao`, `matrizFilial`) | autenticado |
| GET | `/api/estabelecimentos/{cnpjBasico}/{ordem}/{dv}` | autenticado |
| GET | `/api/dominios/{cnaes,municipios,naturezas,paises,qualificacoes,motivos}` | autenticado |
| GET | `/api/exportacao/{empresas,estabelecimentos,cnaes}` (`limite`, filtros; XLSX) | autenticado |

Paginação: `page` (0-based) e `size` (default 20) → `{ content, page, size, totalElements, totalPages }`.
Busca CNAE (compatível com `queries.sql`): principal **ou** secundário, qualificado por `tipoCnae=principal|secundario|ambos`.
Descrições de domínio (município, natureza jurídica) resolvidas na consulta (`LEFT JOIN` no detalhe, lookup em lote nas listas)
e exibidas junto ao código (ex.: `6493 — IBITINGA/SP`).

### 3.5 Importação — concluído

- Lógica em `ImportacaoService` (o runner virou componentes de execução: `CnpjImportExecutor` etc.).
- Configuração lida da tabela `configuracao_importacao` com fallback para propriedades.
- Execução assíncrona com estado em memória; bloqueio de concorrência (`409`).
- Disparo no boot via `app.import.enabled` (default `false`).
- Fluxo: descompacta ZIP em dir temporário → importa via COPY → apaga temporário.

### 3.6 Exportação XLSX — concluído (iteração atual)

- `ExportacaoService` com POI **SXSSF** escrevendo em arquivo temporário; `ExportacaoController` responde
  com `Content-Length` (lição: `StreamingResponseBody` chunked quebra clientes estritos).
- Respeita filtros das telas + parâmetro `limite` (default 10000, mínimo 1).

## 4. Frontend (`frontend/`) — concluído

### 4.1 Estrutura (vigente)

```
src/app/
├── core/{auth, models, services}   # auth.service, guards, token.interceptor, modelos, api.service
├── shared/{paginacao, download}    # paginação + helper de download de Blob
├── pages/                          # login, cadastro, empresas, estabelecimentos, cnaes,
│                                   # importacao, usuarios, layout, home, version
├── app.routes.ts / app.config.ts
└── environments/{environment, environment.prod}.ts   # apiBaseUrl + fileReplacements
```

### 4.2 Rotas (vigentes)

| Rota | Componente | Proteção |
|---|---|---|
| `/login` , `/cadastro` | Login, CadastroUsuario | pública |
| `/empresas`, `/empresas/:cnpjBasico` | EmpresasLista, EmpresaDetalhe | autenticado |
| `/estabelecimentos`, `/estabelecimentos/:cnpjBasico/:ordem/:dv` | EstabelecimentosLista, EstabelecimentoDetalhe | autenticado |
| `/cnaes` | Cnaes | autenticado |
| `/usuarios`, `/importacao` | Usuarios, Importacao | ADMIN |
| `**` | redirect `/empresas` | — |

### 4.3 Consultas e exportação

- Empresas/Estabelecimentos listam com filtros e tabela paginada; links cruzados
  (empresa → estabelecimentos por `cnpjBasico`; CNAE → estabelecimentos por `cnae`+`situacao`).
- Detalhes exibem código + descrição de domínios.
- Cada tela tem botão **Exportar XLSX** (usa `ApiService.blob()` + `shared/download.ts`); Empresas e
  Estabelecimentos têm campo "Limite".

## 5. Faseamento — entregue

| Fase | Entrega | Status |
|---|---|---|
| 1 | Auth + usuários (backend/front) | ✓ |
| 2 | Importação como serviço + telas/status | ✓ |
| 3 | Consultas (empresas/estabelecimentos/dominios) + índices | ✓ |
| 4 | Ajustes finais: testes (25), Swagger, erros, paginação | ✓ |
| 5 | Página CNAE + links cruzados + descrições de domínio + **exportação XLSX** | ✓ |
| 6 | Deploy dev (front-dev.espalha.ai → api-dev.espalha.ai) + CORS | ✓ |

## 6. Testes e critérios de aceite (no lugar)

- Login válido → token; credenciais inválidas → `401`.
- Sem `ADMIN` não acessa `/api/usuarios` nem `/api/importacao/*` (`403`).
- Cadastro com e-mail duplicado → `409`.
- Importação concorrente → `409`.
- Busca por CNAE principal/secundário coerente com `queries.sql`.
- Exportação: `200` com XLSX válido (`PK`), `Content-Length` presente, sem token → `401`.
- Cobertura: `@SpringBootTest(RANDOM_PORT)` + `RestClient` + AssertJ (25 testes).

## 7. Decisões — resolvidas (era "em aberto")

| Decisão | Escolhido |
|---|---|
| Autenticação | JWT stateless (Bearer) |
| Auto-cadastro | Por propriedade; default **desabilitado** (`app.cadastro-publico=false`), admin via seed |
| CNAE secundário | Índice GIN sobre `string_to_array` |
| Disparo da importação | Via API + boot opcional (`app.import.enabled=false`) |
| UI | Tailwind v4 + componentes próprios (sem biblioteca de UI) |
| Repositório | **Monorepo Git único** (`cnpj/` + `frontend/` + `docs/`) |

## 8. Próximos itens (roadmap)

- [ ] CI/CD (GitHub Actions: build, testes, lint, publicação).
- [ ] Testcontainers nos testes de integração (remover dependência do Postgres local).
- [ ] Paginação por cursor/keyset para listagens muito grandes.
- [ ] Autenticação com refresh token + logout server-side.
- [ ] Exportação assíncrona (tarefa em segundo plano + download posterior).
- [ ] Docker Compose (postgres + api + frontend).