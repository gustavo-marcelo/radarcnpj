# --- Runtime: baixa o SPA do último release do GitHub ---
FROM nginx:1.27-alpine

ARG REPO=gustavo-marcelo/radarcnpj
ARG ASSET=radar-cnpj-frontend.zip

RUN apk add --no-cache curl unzip \
    && curl -fsSL -o /tmp/radar-cnpj.zip "https://github.com/${REPO}/releases/latest/download/${ASSET}" \
    && mkdir -p /usr/share/nginx/html \
    && unzip -q /tmp/radar-cnpj.zip -d /usr/share/nginx/html \
    && rm -f /tmp/radar-cnpj.zip \
    && apk del curl unzip

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80