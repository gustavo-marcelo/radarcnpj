-- Busca textual (ILIKE %termo%) apoiada por pg_trgm.
-- Os índices já foram criados manualmente na base importada; IF NOT EXISTS evita reconstrução.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS ix_empresa_razao_social_trgm
	ON empresa USING gin (razao_social gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_estab_nome_fantasia_trgm
	ON estabelecimento USING gin (nome_fantasia gin_trgm_ops);