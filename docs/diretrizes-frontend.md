# Diretrizes de Frontend — Angular

> Documento vivo. Estas diretrizes descrevem **como escrevemos frontend** neste projeto
> (monorepo `frontend/`, app `radar-cnpj`). Onde houver conflito entre "o que é possível" e
> "o que está aqui", este documento vence; exceções são justificadas em code review.

---

## 1. Princípios

1. **O código é lido muito mais vezes do que é escrito.** Otimize para leitura.
2. **Explícito é melhor que implícito.** Sem mágica, sem "funciona na minha máquina".
3. **Reativo por padrão.** Signals para estado de UI; RxJS quando a complexidade assíncrona pedir.
4. **O código mora perto de quem o usa.** Estrutura por `pages/` (telas) + `core`/`shared` transversais.
5. **O componente é a menor unidade coesa.** Evite componentes-deus e componentes-atômicos inúteis.
6. **Segurança e acessibilidade não são features.** São requisitos de qualidade.
7. **Se não tem teste, está quebrado — só ainda não sabemos.**

---

## 2. Stack e versões

| Item | Versão | Observação |
| --- | --- | --- |
| Angular | 22.1.x | Standalone, Signals, control flow nativo (`@if`/`@for`) |
| TypeScript | 6.0.x | `strict` obrigatório |
| Node | LTS (22.x) | Sugerido fixar via `.nvmrc` |
| Tailwind CSS | 4.x | Via PostCSS (`@tailwindcss/postcss`) |
| RxJS | 7.8.x | Interop com Signals |
| Testes | Jasmine + Karma (`@angular/build:karma`) | `npm run test:ci` com ChromeHeadless |
| Lint/format | ESLint (flat) + Prettier | Husky + lint-staged (pre-commit) |
| Commits | Conventional Commits | Validado por commitlint |

> **Atenção (lição aprendida):** manter `jasmine-core` em `~6.x` enquanto os testes rodarem via Karma.
> O Jasmine 7 removeu APIs que o `zone.js` usa, quebrando o runner. Só subir quando migrarmos
> para o builder `unit-test` sem Karma e sem zone.js.

---

## 3. Estrutura de pastas

```text
src/
├── app/
│   ├── core/
│   │   ├── auth/              # auth.service, guards, token.interceptor
│   │   ├── models/            # tipos de domínio (empresa, estabelecimento, usuario, pagina, dominio, ...)
│   │   └── services/          # api.service.ts (único client HTTP base)
│   ├── shared/                # reutilizável e sem estado de domínio
│   │   ├── paginacao/         # componente de paginação
│   │   └── download.ts        # helper de download de Blob
│   ├── pages/                 # cada "page" é uma tela
│   │   ├── home/ version/ layout/ login/ cadastro/
│   │   ├── empresas/          # empresas-lista, empresa-detalhe
│   │   ├── estabelecimentos/  # estabelecimentos-lista, estabelecimento-detalhe
│   │   ├── cnaes/             # consulta CNAE (+ link p/ estabelecimentos)
│   │   ├── usuarios/          # gestão de usuários (ADMIN)
│   │   └── importacao/        # tela de importação (ADMIN)
│   ├── app.config.ts          # providers (zona, router, interceptors, withXhr)
│   ├── app.routes.ts          # rotas + guards
│   ├── app.ts / app.html / app.scss
│   └── app.spec.ts
├── environments/
│   ├── environment.ts         # dev (apiBaseUrl = https://api-dev.espalha.ai/api)
│   └── environment.prod.ts    # produção (mesmo valor; troca via fileReplacements no build)
├── styles.scss                # entrypoint Tailwind
└── main.ts
```

### 3.1 Regras de dependência

```text
core     → transversal; nada depende de core exceto via DI padrão
shared   → NÃO importa de pages/features
pages    → pode importar core e shared; uma page não importa outra page
```

---

## 4. Convenções de nomenclatura

| Artefato | Convenção | Exemplo |
| --- | --- | --- |
| Arquivo | `kebab-case` + sufixo | `empresa-detalhe.ts`, `token.interceptor.ts` |
| Componente (classe) | `PascalCase`, sem sufixo `Component` | `EmpresaDetalhe` |
| Serviço | `PascalCase` + `Service` | `ApiService`, `AuthService` |
| Guard / Interceptor | função sem sufixo | `authGuard`, `adminGuard`, `tokenInterceptor` |
| Signal | substantivo, sem prefixo | `carregando`, `pagina`, `erro` |
| Modelo/DTO | `PascalCase` | `Empresa`, `Estabelecimento`, `Pagina<T>` |
| Método | `camelCase`, verbo no infinitivo | `novaPagina()`, `exportar()` |
| Booleano | prefixo `is`/`has`/`can` | `carregando()` (signal), `buscou` |

**Proibido:** nomes genéricos (`data`, `item`, `temp`, `obj`, `handle`).

---

## 5. Componentes

- **Standalone sempre** (`imports: [...]` no decorator). Sem `NgModule`.
- `ChangeDetectionStrategy.OnPush` obrigatório.
- Estado em `signal()`; Página/erro/loading expostos como signals readonly (`asReadonly()`).
- `@if`, `@for`, `@switch` nativos; **não** usar `*ngIf`/`*ngFor`; `@for` sempre com `track`.
- Formulários de tela com `FormsModule` + `[(ngModel)]` + `name` (padrão atual do projeto;
  a meta para formulários de negócio é **typed reactive forms** — adotar quando um formulário crescer).
- Componentes até ~200 linhas; acima disso, extraia.

---

## 6. Estado e reatividade

| Situação | Ferramenta |
| --- | --- |
| Estado local de UI | `signal()` no componente |
| Estado derivado | `computed()` |
| Chamadas async em handlers | `async/await` + `firstValueFrom(...)` |
| Fluxos reativos (polling, combinação) | RxJS |

Exemplo usado nas telas de lista:

```ts
carregando = signal(false);
pagina = signal<Pagina<Empresa> | null>(null);
erro = signal('');

async buscar(pagina = 0): Promise<void> {
  this.carregando.set(true);
  this.erro.set('');
  try {
    this.pagina.set(await firstValueFrom(this.api.get<Pagina<Empresa>>('/empresas', params)));
  } catch (e) {
    this.pagina.set(null);
    this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao listar.');
  } finally {
    this.carregando.set(false);
  }
}
```

- `erro()` sempre lê `error.detail` (backend usa Problem Details RFC 9457).
- Não usar `effect()` para derivar estado — esse é papel de `computed()`.

---

## 7. Comunicação com a API

- `core/services/api.service.ts`: client único na raiz (`baseUrl = environment.apiBaseUrl`),
  com `get<T>()`, `post<T>()`, `put<T>()`, `delete<T>()` e `blob()` (download XLSX, `responseType: 'blob'`).
- **Interceptor** (`core/auth/token.interceptor.ts`, funcional) injeta `Authorization: Bearer <token>`
  automaticamente — inclusive no `blob()`.
- Providers do `HttpClient`: `provideHttpClient(withInterceptors([tokenInterceptor]), withXhr())`
  (`withXhr()` é necessário no ambiente dev por trás do proxy/HTTPS).
- **Erros**: `HttpErrorResponse` com Problem Details; mensagem de `error.detail` exibida no template.
- **Download de exportação**: `shared/download.ts` → `baixarArquivo(blob, 'empresas.xlsx')`
  (cria `objectURL`, clica no link, revoga).

---

## 8. Roteamento e guards

- `app.routes.ts` com rotas estáticas; `canActivate` com **functional guards** (`authGuard`, `adminGuard`)
  usando `inject()`.
- Rotas: `/login`, `/cadastro`, `/empresas`, `/empresas/:cnpjBasico`,
  `/estabelecimentos`, `/estabelecimentos/:cnpjBasico/:cnpjOrdem/:cnpjDv`, `/cnaes`,
  `/importacao` e `/usuarios` (ADMIN); `**` → `/empresas`.
- Query params de partida lidos com `ActivatedRoute` (`?cnpjBasico=`, `?cnae=`, `?situacao=`).

---

## 9. Estilização (Tailwind CSS v4)

- Utilitários Tailwind aplicados direto no template; SCSS apenas para casos especiais.
- Evite `!important` (`!`); use classes utilitárias consistentes (ex.: `text-indigo-600`, `border-slate-200`).
- Elementos interativos são `<button type="button">`/`<a href>`/formulário, nunca `div` com `(click)`.
- Estados de loading (`disabled` + texto "Buscando…"), erro (caixa vermelha) e vazio (`@empty`) sempre cobertos.

---

## 10. Segurança

- Autorização real é no backend; o frontend só esconde/redireciona.
- **Nunca** guarde segredos/`apiKey` no bundle — tudo no bundle é público.
- Não usar `innerHTML`/`eval`/`Function` com dados dinâmicos.
- Token em memória; em `401` o scanner de sessão redireciona ao login.
- Links externos com `rel="noopener noreferrer"`.

---

## 11. Configuração de ambiente

- `environment.ts` (dev) e `environment.prod.ts` (produção); o `angular.json` troca os arquivos
  no build `production` via `fileReplacements` (config que difere entre ambientes fica lá).
- Config de API: `apiBaseUrl`. Dev também suporta `proxy.conf.json` (`/api` → backend/dev).
- **Sem segredos** em environment (CORS e secret ficam no backend).

---

## 12. Testes

- Jasmine + Karma via `npm run test:ci` (ChromeHeadless, sem watch).
- Testes por comportamento (`deve ...`), um comportamento por teste, sem acoplar a classes Tailwind.

---

## 13. Qualidade, lint e hooks

- **ESLint** (flat) + **Prettier** como única fonte de formatação; `npm run lint` sem warnings.
- **Husky** + **lint-staged**: eslint --fix + prettier nos arquivos staged.
- `strict: true` no `tsconfig`; sem `any`; sem `// eslint-disable` sem justificativa.
- Commitlint exige Conventional Commits (`feat(empresas): ...`, `fix(estabelecimentos): ...`).

---

## 14. Checklist de PR (frontend)

- [ ] `npm run lint` e `npm run test:ci` passam.
- [ ] Componentes `standalone` com `OnPush`; `@for` com `track`; sem `*ngIf`/`*ngFor`.
- [ ] Estados de **loading**, **vazio** e **erro** tratados; `error.detail` usado.
- [ ] Chamadas HTTP via `ApiService` (interceptor injeta o token); sem `HttpClient` solto no componente.
- [ ] Acessível por teclado; `label` correto em formulários; contraste ok.
- [ ] Exportação (se aplicável) via `api.blob()` + `baixarArquivo()`.
- [ ] Sem segredos no bundle; sem `innerHTML` inseguro; sem `console.log`/código morto.
- [ ] Build de produção ok (`npm run build`).