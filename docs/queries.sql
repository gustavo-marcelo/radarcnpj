-- Contagem de registros por tabela
SELECT 'empresa' AS tabela, count(*) AS registros FROM empresa
UNION ALL SELECT 'estabelecimento', count(*) FROM estabelecimento
UNION ALL SELECT 'simples', count(*) FROM simples
UNION ALL SELECT 'pais', count(*) FROM pais
UNION ALL SELECT 'municipio', count(*) FROM municipio
UNION ALL SELECT 'cnae', count(*) FROM cnae
UNION ALL SELECT 'natureza_juridica', count(*) FROM natureza_juridica
UNION ALL SELECT 'qualificacao', count(*) FROM qualificacao
UNION ALL SELECT 'motivo_situacao_cadastral', count(*) FROM motivo_situacao_cadastral
ORDER BY tabela;

-- Tamanho do schema public em KB (tabelas + indices + toast)
SELECT COALESCE(round(sum(pg_total_relation_size(c.oid))::numeric / 1024), 0) AS tamanho_kb
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind IN ('r', 'm', 't', 'p');

-- Empresas que possuem determinado CNAE fiscal principal
-- Informe o código substituindo :cnae (ex.: 4929902)
SELECT DISTINCT emp.cnpj_basico,
       emp.razao_social,
       emp.natureza_juridica,
       emp.porte,
       est.nome_fantasia,
       est.situacao_cadastral,
       est.municipio,
       est.uf
FROM empresa emp
JOIN estabelecimento est ON est.cnpj_basico = emp.cnpj_basico
WHERE est.cnae_fiscal_principal = ':cnae'
ORDER BY emp.razao_social;

-- Empresas que possuem determinado CNAE fiscal principal ou secundário
-- Informe o código substituindo :cnae (ex.: 4929902)
SELECT DISTINCT emp.cnpj_basico,
       emp.razao_social,
       emp.natureza_juridica,
       emp.porte,
       est.nome_fantasia,
       est.situacao_cadastral,
       est.municipio,
       est.uf
FROM empresa emp
JOIN estabelecimento est ON est.cnpj_basico = emp.cnpj_basico
WHERE est.cnae_fiscal_principal = ':cnae'
   OR ':cnae' = ANY(string_to_array(est.cnae_fiscal_secundaria, ','))
ORDER BY emp.razao_social;