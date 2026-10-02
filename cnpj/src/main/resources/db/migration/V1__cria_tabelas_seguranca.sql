CREATE TABLE IF NOT EXISTS usuario (
    id            bigserial PRIMARY KEY,
    nome          varchar(150) NOT NULL,
    email         varchar(200) NOT NULL UNIQUE,
    senha_hash    varchar(100) NOT NULL,
    papel         varchar(20)  NOT NULL DEFAULT 'USER',
    ativo         boolean      NOT NULL DEFAULT true,
    criado_em     timestamptz  NOT NULL DEFAULT now(),
    atualizado_em timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE configuracao_importacao (
    id             smallint PRIMARY KEY DEFAULT 1,
    dados_dir      varchar(500) NOT NULL,
    threads        smallint     NOT NULL DEFAULT 4,
    truncate_antes boolean      NOT NULL DEFAULT true,
    atualizado_em  timestamptz  NOT NULL DEFAULT now()
);

INSERT INTO configuracao_importacao (id, dados_dir, threads, truncate_antes)
VALUES (1, '../dados', 4, true)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE importacao_execucao (
    id            bigserial PRIMARY KEY,
    iniciado_em   timestamptz NOT NULL DEFAULT now(),
    finalizado_em timestamptz,
    estado        varchar(20) NOT NULL,
    linhas_total  bigint,
    mensagem      text
);