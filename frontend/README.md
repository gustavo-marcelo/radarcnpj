# Template Corporativo

Template base para projetos Angular 20 com Tailwind CSS v4, desenvolvido pela Espalha.ai.

## O que este template oferece

- **Angular 20** com componentes standalone (sem NgModules)
- **Tailwind CSS v4** via PostCSS
- **SCSS** como preprocessor de estilos
- **Testes unitários** com Jasmine + Karma + cobertura
- **Linting** com ESLint + angular-eslint
- **Formatador** com Prettier
- **Convenção de commits** com Commitlint (conventional commits)
- **Git Hooks** com Husky (pré-commit executa `npm run test:ci`)
- **lint-staged** — formata e faz lint apenas dos arquivos staged
- **Roteamento** configurado (`/home`, `/version`)
- **Página de versão** com informações do `package.json`

## Pré-requisitos

- Node.js 22+
- npm 10+
- Angular CLI 20 (`npm install -g @angular/cli`)

## Instalação

```bash
# Clonar o repositório
git clone <url-do-repositorio> nome-do-projeto
cd nome-do-projeto

# Instalar dependências
npm install

# Configurar Husky (hooks de git)
npm run prepare
```

## Desenvolvimento

```bash
# Servidor de desenvolvimento (http://localhost:4200)
npm start

# Build de produção
npm run build

# Testes unitários
npm test

# Testes unitários em execução única (usado no pre-commit)
npm run test:ci

# Lint
npm run lint

# Build em modo watch (dev)
npm run watch
```

## Atualização

Para atualizar dependências do template:

```bash
# Verificar versões disponíveis
npm outdated

# Atualizar para as versões mais recentes (respeitando semver)
npm update

# Atualizar para versões principais (major)
npm install @angular/core@latest @angular/cli@latest @angular/compiler-cli@latest @angular/build@latest
npm install tailwindcss@latest @tailwindcss/postcss@latest
npm install typescript@latest

npx npm-check-updates
```

> ⚠️ Ao atualizar versões principais do Angular, consulte o guia oficial de migração em `https://update.angular.dev`.

## Como usar este template em um novo projeto

### Checklist de customização

- [ ] Alterar `name` e `description` no `package.json`
- [ ] Atualizar `src/index.html` (título, meta tags, favicon)
- [ ] Configurar variáveis de ambiente (criar `src/environments/` se necessário)
- [ ] Adicionar proxy para API (`proxy.conf.json`)
- [ ] Configurar CI/CD (GitHub Actions, GitLab CI, etc.)
- [ ] Adicionar Dockerfile se necessário
- [ ] Substituir `/home` e `/version` pelas páginas do projeto real
- [ ] Ajustar cores e tema do Tailwind no `src/styles.scss`
- [ ] Configurar URL do repositório remoto

## Estrutura

```
├── public/                  # Arquivos estáticos
├── src/
│   ├── app/
│   │   ├── pages/
│   │   │   ├── home/        # Página inicial
│   │   │   └── version/     # Página de versão
│   │   ├── app.config.ts    # Providers da aplicação
│   │   ├── app.routes.ts    # Definição de rotas
│   │   └── app.ts           # Componente raiz
│   ├── index.html           # Entry point HTML
│   ├── main.ts              # Bootstrap da aplicação
│   └── styles.scss          # Estilos globais (Tailwind)
├── angular.json             # Configuração do Angular CLI
├── commitlint.config.js     # Regras de commit
├── eslint.config.js         # Configuração do ESLint
├── .husky/                  # Git hooks
├── .prettierrc              # Configuração do Prettier
├── .editorconfig            # Configuração do editor
└── .postcssrc.json          # Configuração do PostCSS
```

## Commits

Este template segue [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: adicionar autenticação JWT
fix: corrigir validação do CPF
docs: atualizar README
style: ajustar identação
refactor: simplificar serviço de login
perf: melhorar consulta ao backend
test: adicionar testes do AuthService
build: atualizar Angular 20
ci: ajustar pipeline do GitHub
chore: atualizar dependências
```
