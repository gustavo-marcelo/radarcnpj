# Manual Docker

Passos rápidos para subir o ambiente e remover tudo ao terminar.

## Subir

```bash
cd docker
docker compose up -d --build
```

- Frontend: http://localhost:4200
- API: http://localhost:8080 (Swagger: http://localhost:8080/swagger-ui.html)
- Admin padrão: `admin@exemplo.com` / `admin123`

## Ver logs

```bash
docker compose logs -f api
docker compose logs -f frontend
```

## Parar (mantendo os dados)

Para os contêineres sem apagar o volume do banco (`cnpj-db-data`):

```bash
docker compose stop
```

Para subir de novo depois: `docker compose up -d`.

## Limpar tudo

Remove contêineres e o volume com os dados do banco:

```bash
docker compose down -v
```

Imagens não são removidas pelo `down`. Para apagar também as imagens criadas:

```bash
docker rmi radar-cnpj-api radar-cnpj-frontend postgres:17-alpine
```

Para uma limpeza completa do Docker (cuidado: afeta todos os projetos):

```bash
docker system prune -a
```

## Observações

- Os dados do banco ficam no volume `cnpj-db-data`; use `down -v` para apagá-los.
- Os `.zip` da Receita em `dados/` não são afetados pelos comandos acima.
- É preciso existir ao menos um Release no GitHub antes do `--build` (o Docker baixa `cnpj.jar` e `radar-cnpj-frontend.zip` dele).