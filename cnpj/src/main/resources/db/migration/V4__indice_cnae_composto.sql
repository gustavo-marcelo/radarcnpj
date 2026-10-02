-- Busca por CNAE principal.
--
-- O índice composto resolve filtro, ordenação (cnpj_basico, cnpj_ordem) e LIMIT no
-- mesmo index scan: a página de 20 linhas sai de 235 s para 8 ms sem tocar no heap
-- além dessas linhas.
--
-- As colunas em INCLUDE (uf, situacao_cadastral, municipio) não participates da
-- ordenação; servem para o count(*) da paginação rodar em index-only scan. Sem elas
-- o planner fazia BitmapAnd entre este índice (1,6M linhas para um CNAE popular) e
-- ix_estab_uf_municipio (19,8M linhas para SP), com páginas de bitmap "lossy"
-- demais: o count de "CNAE + UF" levava 24 s. Com INCLUDE cai para 137 ms.
--
-- Custa 1,2 GB a mais que a versão só com as três colunas de chave.
--
-- Em base com 68M de linhas já importadas, criar fora do Flyway evita travar
-- escrita por minutos:
--   CREATE INDEX CONCURRENTLY ix_estab_cnae_princ_cnpj
--     ON estabelecimento (cnae_fiscal_principal, cnpj_basico, cnpj_ordem)
--     INCLUDE (uf, situacao_cadastral, municipio);
CREATE INDEX IF NOT EXISTS ix_estab_cnae_princ_cnpj
	ON estabelecimento (cnae_fiscal_principal, cnpj_basico, cnpj_ordem)
	INCLUDE (uf, situacao_cadastral, municipio);

-- ix_estab_cnae_princ (só cnae_fiscal_principal) vira prefixo esquerdo redundante.
DROP INDEX IF EXISTS ix_estab_cnae_princ;

-- Trigram em empresa ocupava 74 GB — a maior parte dos 83 GB da tabela — e o planner
-- nunca o escolhia: a busca por razão social acaba em count(*) e ORDER BY sobre 70M
-- linhas, o que descarta o índice de qualquer forma. Removido para liberar disco.
DROP INDEX IF EXISTS ix_empresa_razao_social_trgm;
