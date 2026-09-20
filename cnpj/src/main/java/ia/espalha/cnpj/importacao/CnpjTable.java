package ia.espalha.cnpj.importacao;

import java.util.List;

public enum CnpjTable {

	PAIS("pais", "Paises", List.of(
			new CnpjColumn("codigo", "varchar(3)"),
			new CnpjColumn("descricao", "varchar(200)"))),

	MUNICIPIO("municipio", "Municipios", List.of(
			new CnpjColumn("codigo", "varchar(4)"),
			new CnpjColumn("descricao", "varchar(200)"))),

	CNAE("cnae", "Cnaes", List.of(
			new CnpjColumn("codigo", "varchar(7)"),
			new CnpjColumn("descricao", "varchar(300)"))),

	NATUREZA_JURIDICA("natureza_juridica", "Naturezas", List.of(
			new CnpjColumn("codigo", "varchar(4)"),
			new CnpjColumn("descricao", "varchar(200)"))),

	QUALIFICACAO("qualificacao", "Qualificacoes", List.of(
			new CnpjColumn("codigo", "varchar(2)"),
			new CnpjColumn("descricao", "varchar(200)"))),

	MOTIVO_SITUACAO_CADASTRAL("motivo_situacao_cadastral", "Motivos", List.of(
			new CnpjColumn("codigo", "varchar(2)"),
			new CnpjColumn("descricao", "varchar(200)"))),

	EMPRESA("empresa", "Empresas", List.of(
			new CnpjColumn("cnpj_basico", "varchar(8)"),
			new CnpjColumn("razao_social", "varchar(300)"),
			new CnpjColumn("natureza_juridica", "varchar(4)"),
			new CnpjColumn("qualificacao_responsavel", "varchar(2)"),
			new CnpjColumn("capital_social", "numeric(18,2)", ColumnTransform.DECIMAL_BR),
			new CnpjColumn("porte", "varchar(2)"),
			new CnpjColumn("ente_federativo", "varchar(200)"))),

	ESTABELECIMENTO("estabelecimento", "Estabelecimentos", List.of(
			new CnpjColumn("cnpj_basico", "varchar(8)"),
			new CnpjColumn("cnpj_ordem", "varchar(4)"),
			new CnpjColumn("cnpj_dv", "varchar(2)"),
			new CnpjColumn("identificador_matriz_filial", "varchar(1)"),
			new CnpjColumn("nome_fantasia", "varchar(200)"),
			new CnpjColumn("situacao_cadastral", "varchar(2)"),
			new CnpjColumn("data_situacao_cadastral", "date", ColumnTransform.DATE),
			new CnpjColumn("motivo_situacao_cadastral", "varchar(2)"),
			new CnpjColumn("nome_cidade_exterior", "varchar(200)"),
			new CnpjColumn("pais", "varchar(3)"),
			new CnpjColumn("data_inicio_atividade", "date", ColumnTransform.DATE),
			new CnpjColumn("cnae_fiscal_principal", "varchar(7)"),
			new CnpjColumn("cnae_fiscal_secundaria", "varchar(1000)"),
			new CnpjColumn("tipo_logradouro", "varchar(30)"),
			new CnpjColumn("logradouro", "varchar(300)"),
			new CnpjColumn("numero", "varchar(20)"),
			new CnpjColumn("complemento", "varchar(300)"),
			new CnpjColumn("bairro", "varchar(150)"),
			new CnpjColumn("cep", "varchar(8)"),
			new CnpjColumn("uf", "varchar(2)"),
			new CnpjColumn("municipio", "varchar(4)"),
			new CnpjColumn("ddd1", "varchar(4)"),
			new CnpjColumn("telefone1", "varchar(20)"),
			new CnpjColumn("ddd2", "varchar(4)"),
			new CnpjColumn("telefone2", "varchar(20)"),
			new CnpjColumn("ddd_fax", "varchar(4)"),
			new CnpjColumn("fax", "varchar(20)"),
			new CnpjColumn("correio_eletronico", "varchar(200)"),
			new CnpjColumn("situacao_especial", "varchar(200)"),
			new CnpjColumn("data_situacao_especial", "date", ColumnTransform.DATE))),

	SIMPLES("simples", "Simples", List.of(
			new CnpjColumn("cnpj_basico", "varchar(8)"),
			new CnpjColumn("opcao_simples", "varchar(1)"),
			new CnpjColumn("data_opcao_simples", "date", ColumnTransform.DATE),
			new CnpjColumn("data_exclusao_simples", "date", ColumnTransform.DATE),
			new CnpjColumn("opcao_mei", "varchar(1)"),
			new CnpjColumn("data_opcao_mei", "date", ColumnTransform.DATE),
			new CnpjColumn("data_exclusao_mei", "date", ColumnTransform.DATE)));

	private final String tableName;
	private final String zipPrefix;
	private final List<CnpjColumn> columns;

	CnpjTable(String tableName, String zipPrefix, List<CnpjColumn> columns) {
		this.tableName = tableName;
		this.zipPrefix = zipPrefix;
		this.columns = List.copyOf(columns);
	}

	public String tableName() {
		return tableName;
	}

	public String zipPrefix() {
		return zipPrefix;
	}

	public List<CnpjColumn> columns() {
		return columns;
	}
}