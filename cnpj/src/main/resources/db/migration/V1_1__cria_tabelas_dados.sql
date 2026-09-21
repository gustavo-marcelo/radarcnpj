-- Tabelas de dados da Receita (layout de.cnjp). Sem elas o boot falha em banco
-- vazio, pois V2/V3 criam índices nestas tabelas. O CnpjImportExecutor também as
-- cria (CREATE TABLE IF NOT EXISTS) antes do COPY; aqui garantimos a existência
-- antes dos índices. IF NOT EXISTS torna esta migration um no-op em bases já
-- importadas (baseline-on-migrate).

CREATE TABLE IF NOT EXISTS pais (
    codigo    varchar(3),
    descricao varchar(200)
);

CREATE TABLE IF NOT EXISTS municipio (
    codigo    varchar(4),
    descricao varchar(200)
);

CREATE TABLE IF NOT EXISTS cnae (
    codigo    varchar(7),
    descricao varchar(300)
);

CREATE TABLE IF NOT EXISTS natureza_juridica (
    codigo    varchar(4),
    descricao varchar(200)
);

CREATE TABLE IF NOT EXISTS qualificacao (
    codigo    varchar(2),
    descricao varchar(200)
);

CREATE TABLE IF NOT EXISTS motivo_situacao_cadastral (
    codigo    varchar(2),
    descricao varchar(200)
);

CREATE TABLE IF NOT EXISTS empresa (
    cnpj_basico              varchar(8),
    razao_social             varchar(300),
    natureza_juridica        varchar(4),
    qualificacao_responsavel varchar(2),
    capital_social           numeric(18,2),
    porte                    varchar(2),
    ente_federativo          varchar(200)
);

CREATE TABLE IF NOT EXISTS estabelecimento (
    cnpj_basico                varchar(8),
    cnpj_ordem                 varchar(4),
    cnpj_dv                    varchar(2),
    identificador_matriz_filial varchar(1),
    nome_fantasia              varchar(200),
    situacao_cadastral         varchar(2),
    data_situacao_cadastral    date,
    motivo_situacao_cadastral  varchar(2),
    nome_cidade_exterior       varchar(200),
    pais                       varchar(3),
    data_inicio_atividade      date,
    cnae_fiscal_principal      varchar(7),
    cnae_fiscal_secundaria     varchar(1000),
    tipo_logradouro            varchar(30),
    logradouro                 varchar(300),
    numero                     varchar(20),
    complemento                varchar(300),
    bairro                     varchar(150),
    cep                        varchar(8),
    uf                         varchar(2),
    municipio                  varchar(4),
    ddd1                       varchar(4),
    telefone1                  varchar(20),
    ddd2                       varchar(4),
    telefone2                  varchar(20),
    ddd_fax                    varchar(4),
    fax                        varchar(20),
    correio_eletronico         varchar(200),
    situacao_especial          varchar(200),
    data_situacao_especial     date
);

CREATE TABLE IF NOT EXISTS simples (
    cnpj_basico           varchar(8),
    opcao_simples         varchar(1),
    data_opcao_simples    date,
    data_exclusao_simples date,
    opcao_mei             varchar(1),
    data_opcao_mei        date,
    data_exclusao_mei     date
);