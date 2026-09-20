CREATE INDEX IF NOT EXISTS ix_empresa_cnpj_basico       ON empresa (cnpj_basico);
CREATE INDEX IF NOT EXISTS ix_estab_cnpj_basico        ON estabelecimento (cnpj_basico);
CREATE INDEX IF NOT EXISTS ix_estab_cnae_princ         ON estabelecimento (cnae_fiscal_principal);
CREATE INDEX IF NOT EXISTS ix_estab_uf_municipio       ON estabelecimento (uf, municipio);
CREATE INDEX IF NOT EXISTS ix_estab_municipio          ON estabelecimento (municipio);
CREATE INDEX IF NOT EXISTS ix_estab_situacao           ON estabelecimento (situacao_cadastral);
CREATE INDEX IF NOT EXISTS ix_estab_cnae_sec_gin
    ON estabelecimento USING gin (string_to_array(cnae_fiscal_secundaria, ','));