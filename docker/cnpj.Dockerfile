# --- Runtime: baixa o jar do último release do GitHub ---
FROM eclipse-temurin:25-jre

ARG REPO=gustavo-marcelo/radarcnpj
ARG ASSET=cnpj.jar

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN mkdir -p /dados \
    && curl -fsSL -o /app/app.jar "https://github.com/${REPO}/releases/latest/download/${ASSET}"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]