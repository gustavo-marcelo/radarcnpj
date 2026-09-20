# Diretrizes de Backend — Java + Spring Boot + PostgreSQL

> Documento vivo. Estas diretrizes definem **como construímos o backend** deste projeto,
> refletindo o que foi implementado e as lições aprendidas. Quando algo não se aplicar,
> a discussão acontece no PR e a decisão, se durável, é refletida aqui.

---

## 1. Princípios

1. **Domínio no centro.** Framework, banco e HTTP são detalhes; o negócio não deve depender deles.
2. **Falhe rápido e falhe claro.** Erro silencioso é dívida técnica disfarçada de estabilidade.
3. **Contrato antes de código.** A API é modelada antes; springdoc gera o OpenAPI a partir do código.
4. **Explícito > esperto.** SQL explícito e parametrizado vence "mágica" de ORM em leitura pesada.
5. **Resiliência por padrão.** Volumes de ~70M empresas / ~68M estabelecimentos exigem limites, índices e streaming.
6. **Observabilidade é requisito.** Actuator com `health` e `info`.
7. **Segurança não é opcional.** JWT, BCrypt, autorização por papel e CORS explícito.

---

## 2. Stack e versões

| Item | Versão | Observação |
| --- | --- | --- |
| Java | 25 (LTS) | Records, pattern matching, virtual threads |
| Spring Boot | 4.1.1 | Spring Framework 7, Jakarta EE 11 |
| Build | Maven (wrapper `./mvnw`) | Reproduzível; não dependa de Maven global |
| Banco | PostgreSQL | Produção local: ~70M empresas, ~68M estabelecimentos |
| Migrations | Flyway | `baseline-on-migrate` (base legada já populada) |
| Acesso a dados | Spring JDBC (`JdbcTemplate`/`NamedParameterJdbcTemplate`) | Decisão deliberada: SQL explícito + `COPY` na carga |
| Autenticação | Spring Security + JWT (`jjwt` 0.12) | Token `Bearer`, senhas BCrypt |
| Documentação | springdoc-openapi 3.1.1 | `/swagger-ui.html`, `/v3/api-docs` |
| Exportação | Apache POI 5.4 (`poi-ooxml`, SXSSF) | XLSX em streaming |
| Observabilidade | Actuator | `health`, `info` |
| Testes | JUnit 5 + AssertJ + RestClient | Integração contra Postgres real (porta aleatória) |

### Decisões deliberadas (registradas em code review)

- **Sem JPA/Hibernate.** Leitura pesada com SQL explícito + índices; carga com `COPY`.
  JPA só entraria se houvesse escrita transacional de volume baixo com valor real de mapeamento.
- **Sem MapStruct.** DTOs são `record` com mapeamento manual curto (em geral projeção direta do `RowMapper`).
- **Sem Testcontainers (por enquanto).** Testes de integração usam o Postgres local (`RANDOM_PORT`).
  Migrar quando houver CI dedicada, eliminando a dependência de banco na máquina.

---

## 3. Arquitetura

Arquitetura **por feature com camadas finas** (Controller → Service → Repository), sem hexagonal formal.
CRUD/consulta e telas administrativas não justificam 5 camadas por recurso. A regra:

> Comece simples e extraia complexidade quando ela aparecer.

```text
Controller  ->  Service       ->  Repository (JdbcTemplate)  ->  PostgreSQL
   |              |                     |
   DTO            regras leves          SQL explícito + índices
   (record)       orquestra/filtros
```

- **Controller** não conhece SQL nem entidades do banco — só DTOs.
- **Service** carrega regras leves e orquestra repositórios (ex.: resolução de descrições de domínio em lote).
- **Repository** é dedicado a SQL (filtros dinâmicos, paginação, exportação).
- **shared/** abriga o transversal: tratamento de erros (`ProblemDetail`) e `Cnpj`.

---

## 4. Estrutura de pastas

Package base: `ia.espalha.cnpj`.

```text
src/main/java/ia/espalha/cnpj/
├── auth/          # JwtService, JwtAuthenticationFilter, AuthController, DTOs
├── usuario/       # Usuario, repositório, UsuarioController (ADMIN), DTOs
├── consulta/      # Empresa/Estabelecimento/Dominio controllers, ExportacaoController/Service,
│                  # repositórios de consulta, DTOs
├── importacao/    # ImportacaoService, Importacao/ConfigImportacao controllers,
│                  # CnpjImportExecutor, CnpjTable/CnpjColumn, CsvTransformReader
├── config/        # SecurityConfig, OpenApiConfig
└── shared/
    ├── domain/    # Cnpj (value object), etc.
    └── error/     # GlobalExceptionHandler + exceções de domínio (Problem Details)
resources/db/migration/  # Flyway: V1 segurança, V2 índices, V3 pg_trgm, ...
```

---

## 5. Padrões de consulta

### 5.1 SQL em vez de ORM

- `NamedParameterJdbcTemplate` com SQL explícito: plano de execução previsível e proveito dos índices.
- Filtros dinâmicos (`WHERE 1=1 AND ...`) com parâmetros nomeados — **nunca concatenar valores**.
- Paginação `OFFSET/LIMIT` (`page`, `size`, default 20, com teto).
- Descrições de domínio (município, natureza jurídica etc.): `LEFT JOIN` no detalhe e **lookup em lote** (`IN (...)` com mapa código→descrição) nas listas para evitar N+1.

### 5.2 Índices

- Índices nas colunas de `WHERE`/`JOIN` relevantes (`V2__indices_consulta.sql`).
- Busca textual por termo parcial (`ILIKE %termo%`) com **`pg_trgm`** (`V3`).
- CNAE secundária em texto separado por vírgula → índice **GIN** sobre `string_to_array(...)`.

### 5.3 Exportação XLSX (lição aprendida)

- POI **SXSSF** (streaming, baixo uso de memória) escreve em **arquivo temporário**.
- Resposta com `Content-Length` explícito via `FileSystemResource` + `Content-Disposition: attachment`;
  o arquivo temporário é limpo após o envio (com fallback `deleteOnExit`).
- **Não** usar `StreamingResponseBody` puro: resposta chunked sem chunk terminal quebra clientes
  HTTP estritos (Java HttpClient/RestClient) com `EOF reached while reading`. Com `Content-Length`
  funciona para qualquer cliente (browser, curl, bibliotecas).

---

## 6. API REST

- Rotas no plural sob `/api`: `/api/empresas`, `/api/estabelecimentos`, `/api/dominios`, `/api/exportacao`,
  `/api/importacao`, `/api/usuarios`, `/api/auth`. (Sem `/v1` por enquanto.)
- Listagens paginadas (`page` + `size`); detalhe por identidade natural (`{cnpjBasico}`, `{cnpjBasico}/{ordem}/{dv}`).
- Resposta de listagem: `{ content, page, size, totalElements, totalPages }`.
- Nunca exponha entidades do banco; use DTOs `record`.

### 6.1 Códigos de status

| Caso | Status |
| --- | --- |
| Sucesso | `200` / `201` |
| Validação | `400` |
| Não autenticado | `401` |
| Sem permissão | `403` |
| Não encontrado | `404` |
| Conflito (e-mail duplicado, importação em execução) | `409` |
| Erro inesperado | `500` |

### 6.2 Erros no padrão RFC 9457 (Problem Details)

`GlobalExceptionHandler` devolve `application/problem+json` com `title`, `status` e `detail` a partir das
exceções de domínio (`EntidadeNaoEncontradaException`, `ConflitoException`, `CredenciaisInvalidasException`).
O frontend lê `error.detail` para exibir mensagens amigáveis. Nunca vazar stack trace/SQL para o cliente.

---

## 7. Segurança

- **Spring Security**: `/api/auth/login` (e `/api/auth/registrar` se `app.cadastro-publico=true`) públicas;
  o restante exige JWT.
- **JWT stateless** (`Authorization: Bearer <token>`) com jjwt; segredo/expiração configuráveis.
- Senhas com **BCrypt**.
- Papéis **USER / ADMIN**: gestão de usuários e importação exigem `ADMIN`.
- **CORS** explícito por origem (`app.cors.origens`); nunca `*` com credenciais.

---

## 8. Configuração

- `application.properties` com sobrescrita por variáveis de ambiente (relaxed binding):
  `SPRING_DATASOURCE_*`, `APP_JWT_SECRET`, `APP_JWT_EXPIRATION_SECONDS`, `APP_CORS_ORIGENS`,
  `APP_CADASTRO_PUBLICO`, `APP_DADOS_DIR`, `APP_IMPORT_ENABLED`, `APP_IMPORT_THREADS`, `APP_IMPORT_DATABASE`.
- Admin inicial em `app.admin.email`/`app.admin.senha` (criado no boot se ausente).
- Configuração da importação pode viver na tabela `configuracao_importacao` (fallback para propriedades).
- **Segredos nunca no Git** — em produção o JWT secret vem de variável de ambiente.

---

## 9. Importação de dados

- Arquivos `.zip` da Receita em `dados/` (configurável): `Empresas*.zip`, `Estabelecimentos*.zip`, ...
  (cada `Compressed=N` gera múltiplos volumes).
- Fluxo por tabela: extração do ZIP para diretório temporário → parsing CSV em streaming → carga delimitada → limpeza.
- Execução **assíncrona** (`ExecutorService` nomeado, threads configuráveis); estado em memória
  (`IDLE/RUNNING/DONE/ERROR`) com tabela/arquivo/linhas atuais.
- **Bloqueio de execução concorrente** (segunda chamada → `409`).
- Disparo via API (`/api/importacao/iniciar`) ou no boot (`app.import.enabled`, default `false`).

---

## 10. Observabilidade e logging

- SLF4J + Logback; nunca `System.out` nem `printStackTrace()`.
- **Nunca logue dados sensíveis** — tokens, senha em claro, PII completa.
- Actuator expõe `health` e `info` sem detalhes sensíveis.

---

## 11. Testes

- Integração `@SpringBootTest(webEnvironment = RANDOM_PORT)` contra Postgres real, com `RestClient` e AssertJ.
  25 testes cobrindo auth (401/403), consultas, fluxos de erro e exportação XLSX.
- **Lição (ambiente de dev):** o Java LS/IDE pode injetar classes quebradas em `target/classes`.
  Rodar sempre `./mvnw -B clean test` antes de concluir — nunca confiar em build incremental da IDE.
- **Lição (JdbcTemplate):** lambdas de expressão em `query` são ambíguas entre `ResultSetExtractor` e
  `RowCallbackHandler` — usar lambda de **bloco** (ou downcast explícito).

---

## 12. Git e commits

- **Conventional Commits**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
- Commits pequenos e focados; um PR = uma intenção.
- Nunca commite segredos, `target/`, `.env`, `node_modules`, `dist` nem os arquivos de `dados/` (gitignored).

---

## 13. Checklist de PR (backend)

- [ ] `./mvnw -B clean test` passa.
- [ ] Rotas/contrato refletidos no OpenAPI (springdoc) e no README.
- [ ] DTOs `record`; sem entidade/SQL vazando no controller.
- [ ] Consultas parametrizadas; índices considerados quando aplicável.
- [ ] Exportação (se aplicável) via arquivo temporário + `Content-Length`.
- [ ] Autorização verificada (rotas `ADMIN`).
- [ ] Erros no padrão Problem Details, sem stack trace para o cliente.
- [ ] Sem `System.out`, código comentado ou segredos.
- [ ] Validação final: `./mvnw -B clean spring-boot:run` + teste ao vivo via curl.