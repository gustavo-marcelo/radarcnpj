package ia.espalha.cnpj.consulta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import ia.espalha.cnpj.consulta.dto.EstabelecimentoDetalheResponse;
import ia.espalha.cnpj.consulta.dto.EstabelecimentoResponse;
import ia.espalha.cnpj.shared.domain.PaginaResponse;

@Repository
public class EstabelecimentoConsultaRepository {

	private static final String COLUNAS = """
			est.cnpj_basico, est.cnpj_ordem, est.cnpj_dv, est.identificador_matriz_filial,
			est.nome_fantasia, est.situacao_cadastral, est.data_situacao_cadastral,
			est.motivo_situacao_cadastral, est.cnae_fiscal_principal, est.cnae_fiscal_secundaria,
			est.logradouro, est.numero, est.complemento, est.bairro, est.cep,
			est.uf, est.municipio, est.ddd1, est.telefone1, est.correio_eletronico""";

	private static final String FROM = """
			FROM estabelecimento est""";

	private final JdbcTemplate jdbc;

	public EstabelecimentoConsultaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<EstabelecimentoResponse> buscarPorEmpresa(String cnpjBasico) {
		List<Linha> linhas = jdbc.query(
				"SELECT " + COLUNAS + " " + FROM
						+ " WHERE est.cnpj_basico = ? ORDER BY est.cnpj_ordem",
				(rs, rowNum) -> toLinha(rs),
				cnpjBasico);
		return preencherEmpresa(linhas);
	}

	public java.util.Optional<EstabelecimentoDetalheResponse> buscarDetalhe(
			String cnpjBasico, String cnpjOrdem, String cnpjDv) {
		List<EstabelecimentoDetalheLinha> linhas = jdbc.query(
				"SELECT "
						+ "est.cnpj_basico, est.cnpj_ordem, est.cnpj_dv, est.identificador_matriz_filial, "
						+ "est.nome_fantasia, est.situacao_cadastral, est.data_situacao_cadastral, "
						+ "est.motivo_situacao_cadastral, est.data_inicio_atividade, est.pais, "
						+ "est.cnae_fiscal_principal, est.cnae_fiscal_secundaria, "
						+ "est.tipo_logradouro, est.logradouro, est.numero, est.complemento, est.bairro, est.cep, "
						+ "est.uf, est.municipio, est.nome_cidade_exterior, "
						+ "m.descricao AS municipio_descricao, "
						+ "est.ddd1, est.telefone1, est.ddd2, est.telefone2, est.ddd_fax, est.fax, "
						+ "est.correio_eletronico, est.situacao_especial, est.data_situacao_especial, "
						+ "emp.razao_social, emp.natureza_juridica, emp.qualificacao_responsavel, "
						+ "emp.capital_social, emp.porte, emp.ente_federativo, "
						+ "nj.descricao AS natureza_juridica_descricao "
						+ "FROM estabelecimento est "
						+ "LEFT JOIN empresa emp ON emp.cnpj_basico = est.cnpj_basico "
						+ "LEFT JOIN natureza_juridica nj ON nj.codigo = emp.natureza_juridica "
						+ "LEFT JOIN municipio m ON m.codigo = est.municipio "
						+ "WHERE est.cnpj_basico = ? AND est.cnpj_ordem = ? AND est.cnpj_dv = ?",
				(rs, rowNum) -> toDetalhe(rs),
				cnpjBasico, cnpjOrdem, cnpjDv);
		if (linhas.isEmpty()) {
			return java.util.Optional.empty();
		}
		EstabelecimentoDetalheLinha linha = linhas.get(0);
		String descricaoCnae = null;
		if (StringUtils.hasText(linha.cnaeFiscalPrincipal())) {
			descricaoCnae = jdbc.query("SELECT descricao FROM cnae WHERE codigo = ?",
					rs -> rs.next() ? rs.getString(1) : null,
					linha.cnaeFiscalPrincipal());
		}
		return java.util.Optional.of(linha.toResponse(descricaoCnae));
	}

	public PaginaResponse<EstabelecimentoResponse> buscar(String cnpjBasico, String nomeFantasia, String cnae,
			String tipoCnae, String uf, String municipio, String situacao, String matrizFilial,
			int page, int size) {
		Filtros f = montarFiltros(cnpjBasico, nomeFantasia, uf, municipio, situacao, matrizFilial);

		if (StringUtils.hasText(cnae)) {
			Cnae c = montarCnae(cnae.trim(),
					StringUtils.hasText(tipoCnae) ? tipoCnae.trim() : "ambos");
			return buscarComCnae(c.sql(), c.params().toArray(), f, page, size);
		}

		long total = " WHERE TRUE".contentEquals(f.where())
				? totalEstimado()
				: jdbc.queryForObject("SELECT count(*) " + FROM + f.where(), Long.class, f.params().toArray());
		List<Object> params = new ArrayList<>(f.params());
		params.add(size);
		params.add((long) page * size);
		List<Linha> linhas = jdbc.query(
				"SELECT " + COLUNAS + " " + FROM + f.where()
						+ " ORDER BY est.cnpj_basico, est.cnpj_ordem LIMIT ? OFFSET ?",
				(rs, rowNum) -> toLinha(rs),
				params.toArray());
		return PaginaResponse.of(preencherEmpresa(linhas), page, size, total);
	}

	public List<EstabelecimentoResponse> exportar(String cnpjBasico, String nomeFantasia, String cnae,
			String tipoCnae, String uf, String municipio, String situacao, String matrizFilial, int limite) {
		Filtros f = montarFiltros(cnpjBasico, nomeFantasia, uf, municipio, situacao, matrizFilial);
		if (StringUtils.hasText(cnae)) {
			Cnae c = montarCnae(cnae.trim(),
					StringUtils.hasText(tipoCnae) ? tipoCnae.trim() : "ambos");
			String join = FROM + " JOIN (" + c.sql() + ") sel"
					+ " ON est.cnpj_basico = sel.cnpj_basico AND est.cnpj_ordem = sel.cnpj_ordem";
			Object[] params = concat(concat(c.params().toArray(), f.params().toArray()),
					new Object[] { limite });
			List<Linha> linhas = jdbc.query(
					"SELECT " + COLUNAS + " " + join + f.where()
							+ " ORDER BY est.cnpj_basico, est.cnpj_ordem LIMIT ?",
					(rs, rowNum) -> toLinha(rs),
					params);
			return preencherEmpresa(linhas);
		}
		List<Object> params = new ArrayList<>(f.params());
		params.add(limite);
		List<Linha> linhas = jdbc.query(
				"SELECT " + COLUNAS + " " + FROM + f.where()
						+ " ORDER BY est.cnpj_basico, est.cnpj_ordem LIMIT ?",
				(rs, rowNum) -> toLinha(rs),
				params.toArray());
		return preencherEmpresa(linhas);
	}

	private record Filtros(String where, List<Object> params) {
	}

	private record Cnae(String sql, List<Object> params) {
	}

	private Filtros montarFiltros(String cnpjBasico, String nomeFantasia, String uf, String municipio,
			String situacao, String matrizFilial) {
		StringBuilder where = new StringBuilder(" WHERE TRUE");
		List<Object> params = new ArrayList<>(6);
		if (StringUtils.hasText(cnpjBasico)) {
			where.append(" AND est.cnpj_basico = ?");
			params.add(cnpjBasico.trim());
		}
		if (StringUtils.hasText(nomeFantasia)) {
			where.append(" AND est.nome_fantasia ILIKE ?");
			params.add("%" + nomeFantasia.trim() + "%");
		}
		if (StringUtils.hasText(uf)) {
			where.append(" AND est.uf = ?");
			params.add(uf.trim().toUpperCase(Locale.ROOT));
		}
		if (StringUtils.hasText(municipio)) {
			where.append(" AND est.municipio = ?");
			params.add(municipio.trim());
		}
		if (StringUtils.hasText(situacao)) {
			where.append(" AND est.situacao_cadastral = ?");
			params.add(situacao.trim());
		}
		if (StringUtils.hasText(matrizFilial)) {
			where.append(" AND est.identificador_matriz_filial = ?");
			params.add(matrizFilial.trim());
		}
		return new Filtros(where.toString(), params);
	}

	private Cnae montarCnae(String codigo, String tipo) {
		List<Object> args = new ArrayList<>(2);
		String inner;
		if (tipo.equals("principal")) {
			inner = "SELECT cnpj_basico, cnpj_ordem FROM estabelecimento WHERE cnae_fiscal_principal = ?";
			args.add(codigo);
		} else if (tipo.equals("secundario")) {
			inner = "SELECT cnpj_basico, cnpj_ordem FROM estabelecimento"
					+ " WHERE string_to_array(cnae_fiscal_secundaria, ',') @> ARRAY[?]::text[]";
			args.add(codigo);
		} else {
			// "ambos" (ou tipoCnae inválido): UNION ALL dos dois índices
			inner = "(SELECT cnpj_basico, cnpj_ordem FROM estabelecimento WHERE cnae_fiscal_principal = ?)"
					+ " UNION ALL "
					+ "(SELECT cnpj_basico, cnpj_ordem FROM estabelecimento"
					+ " WHERE string_to_array(cnae_fiscal_secundaria, ',') @> ARRAY[?]::text[])";
			args.add(codigo);
			args.add(codigo);
		}
		return new Cnae(inner, args);
	}

	/**
	 * Filtro por CNAE: o conjunto de candidatas é pequeno, então primeiro resolve-se
	 * no índice apropriado (btree do principal / GIN da secundária) e depois
	 * aplicam-se os demais filtros no JOIN. Evita que o planner opte por seq scan da
	 * tabela de 68M de linhas ao combinar CNAE com outras colunas.
	 */
	private PaginaResponse<EstabelecimentoResponse> buscarComCnae(String inner, Object[] argsInner,
			Filtros f, int page, int size) {
		String join = FROM + " JOIN (" + inner + ") sel"
				+ " ON est.cnpj_basico = sel.cnpj_basico AND est.cnpj_ordem = sel.cnpj_ordem";
		Object[] targs = concat(argsInner, f.params().toArray());
		long total = jdbc.queryForObject("SELECT count(*) " + join + f.where(), Long.class, targs);
		// MATERIALIZED isola o LIMIT/ORDER BY do conjunto pequeno de candidatas:
		// sem isso o planner tenta "ordenar com ponto de partida" varrendo o índice
		// global de cnpj_basico (68M de linhas) em vez de usar o índice do CNAE.
		Object[] cparams = concat(concat(argsInner, f.params().toArray()), new Object[] { size, (long) page * size });
		String materializado = "WITH pag AS MATERIALIZED (SELECT " + COLUNAS + " " + join + f.where() + ")";
		List<Linha> linhas = jdbc.query(
				materializado + " SELECT * FROM pag ORDER BY cnpj_basico, cnpj_ordem LIMIT ? OFFSET ?",
				(rs, rowNum) -> toLinha(rs),
				cparams);
		return PaginaResponse.of(preencherEmpresa(linhas), page, size, total);
	}

	/**
	 * Sem filtros, o count(*) sobre 68M de linhas é caro; usa a estimativa do
	 * planner (reltuples) que o importa/profiler mantém razoavelmente atualizada.
	 */
	private long totalEstimado() {
		Long estimado = jdbc.queryForObject(
				"SELECT reltuples::bigint FROM pg_class WHERE relname = 'estabelecimento'", Long.class);
		return estimado == null || estimado <= 0 ? 0 : estimado;
	}

	/**
	 * Busca em lote razao_social/porte da empresa para as linhas da página. Evita
	 * o LEFT JOIN da segunda maior tabela do banco nos planos de contagem/ordenação.
	 */
	private List<EstabelecimentoResponse> preencherEmpresa(List<Linha> linhas) {
		if (linhas.isEmpty()) {
			return List.of();
		}
		List<String> chaves = linhas.stream()
				.map(Linha::cnpjBasico)
				.filter(StringUtils::hasText)
				.distinct()
				.toList();
		Map<String, String[]> porCnpj = new HashMap<>();
		if (!chaves.isEmpty()) {
			String in = String.join(",", java.util.Collections.nCopies(chaves.size(), "?"));
			jdbc.query(
				"SELECT cnpj_basico, razao_social, porte FROM empresa WHERE cnpj_basico IN (" + in + ")",
				rs -> {
					porCnpj.put(rs.getString("cnpj_basico"),
							new String[] { rs.getString("razao_social"), rs.getString("porte") });
				},
				chaves.toArray());
		}
		Map<String, String> porMunicipio = descricoesPorCodigo("municipio",
				linhas.stream().map(Linha::municipio).toList());
		return linhas.stream()
				.map(l -> {
					String[] emp = porCnpj.get(l.cnpjBasico());
					return l.toResponse(
							emp == null ? null : emp[0],
							emp == null ? null : emp[1],
							porMunicipio.get(l.municipio()));
				})
				.toList();
	}

	private Map<String, String> descricoesPorCodigo(String tabela, List<String> codigos) {
		Map<String, String> porCodigo = new HashMap<>();
		List<String> distintos = codigos.stream()
				.map(codigo -> codigo == null ? "" : codigo.trim())
				.filter(StringUtils::hasText)
				.distinct()
				.toList();
		if (distintos.isEmpty()) {
			return porCodigo;
		}
		String in = String.join(",", java.util.Collections.nCopies(distintos.size(), "?"));
		jdbc.query("SELECT codigo, descricao FROM " + tabela + " WHERE codigo IN (" + in + ")",
				rs -> {
					porCodigo.put(rs.getString("codigo"), rs.getString("descricao"));
				},
				distintos.toArray());
		return porCodigo;
	}

	private static Object[] concat(Object[] a, Object[] b) {
		Object[] r = new Object[a.length + b.length];
		System.arraycopy(a, 0, r, 0, a.length);
		System.arraycopy(b, 0, r, a.length, b.length);
		return r;
	}

	private static Linha toLinha(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new Linha(
				rs.getString("cnpj_basico"),
				rs.getString("cnpj_ordem"),
				rs.getString("cnpj_dv"),
				rs.getString("identificador_matriz_filial"),
				rs.getString("nome_fantasia"),
				rs.getString("situacao_cadastral"),
				rs.getObject("data_situacao_cadastral", LocalDate.class),
				rs.getString("motivo_situacao_cadastral"),
				rs.getString("cnae_fiscal_principal"),
				rs.getString("cnae_fiscal_secundaria"),
				rs.getString("logradouro"),
				rs.getString("numero"),
				rs.getString("complemento"),
				rs.getString("bairro"),
				rs.getString("cep"),
				rs.getString("uf"),
				rs.getString("municipio"),
				rs.getString("ddd1"),
				rs.getString("telefone1"),
				rs.getString("correio_eletronico"));
	}

	private record Linha(
			String cnpjBasico, String cnpjOrdem, String cnpjDv, String identificadorMatrizFilial,
			String nomeFantasia, String situacaoCadastral, LocalDate dataSituacaoCadastral,
			String motivoSituacaoCadastral, String cnaeFiscalPrincipal, String cnaeFiscalSecundaria,
			String logradouro, String numero, String complemento, String bairro, String cep,
			String uf, String municipio, String ddd1, String telefone1, String correioEletronico) {

		EstabelecimentoResponse toResponse(String razaoSocial, String porte, String municipioDescricao) {
			String completo = Objects.toString(cnpjBasico, "") + Objects.toString(cnpjOrdem, "")
					+ Objects.toString(cnpjDv, "");
			return new EstabelecimentoResponse(
					cnpjBasico, cnpjOrdem, cnpjDv, completo, identificadorMatrizFilial, nomeFantasia,
					situacaoCadastral, dataSituacaoCadastral, motivoSituacaoCadastral,
					cnaeFiscalPrincipal, cnaeFiscalSecundaria, logradouro, numero, complemento,
					bairro, cep, uf, municipio, municipioDescricao, ddd1, telefone1, correioEletronico,
					razaoSocial, porte);
		}
	}

	private record EstabelecimentoDetalheLinha(
			String cnpjBasico, String cnpjOrdem, String cnpjDv, String identificadorMatrizFilial,
			String nomeFantasia, String situacaoCadastral, LocalDate dataSituacaoCadastral,
			String motivoSituacaoCadastral, LocalDate dataInicioAtividade, String pais,
			String cnaeFiscalPrincipal, String cnaeFiscalSecundaria,
			String tipoLogradouro, String logradouro, String numero, String complemento,
			String bairro, String cep, String uf, String municipio, String municipioDescricao,
			String nomeCidadeExterior,
			String ddd1, String telefone1, String ddd2, String telefone2, String dddFax, String fax,
			String correioEletronico, String situacaoEspecial, LocalDate dataSituacaoEspecial,
			String razaoSocial, String naturezaJuridica, String naturezaJuridicaDescricao,
			String qualificacaoResponsavel,
			java.math.BigDecimal capitalSocial, String porte, String enteFederativo) {

		EstabelecimentoDetalheResponse toResponse(String descricaoCnae) {
			String completo = Objects.toString(cnpjBasico, "") + Objects.toString(cnpjOrdem, "")
					+ Objects.toString(cnpjDv, "");
			return new EstabelecimentoDetalheResponse(
					cnpjBasico, cnpjOrdem, cnpjDv, completo, identificadorMatrizFilial, nomeFantasia,
					situacaoCadastral, dataSituacaoCadastral, motivoSituacaoCadastral,
					dataInicioAtividade, pais, cnaeFiscalPrincipal, cnaeFiscalSecundaria, descricaoCnae,
					tipoLogradouro, logradouro, numero, complemento, bairro, cep, uf, municipio,
					municipioDescricao, nomeCidadeExterior, ddd1, telefone1, ddd2, telefone2, dddFax, fax,
					correioEletronico, situacaoEspecial, dataSituacaoEspecial,
					razaoSocial, porte, naturezaJuridica, naturezaJuridicaDescricao,
					qualificacaoResponsavel, capitalSocial,
					enteFederativo);
		}
	}

	private static EstabelecimentoDetalheLinha toDetalhe(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new EstabelecimentoDetalheLinha(
				rs.getString("cnpj_basico"),
				rs.getString("cnpj_ordem"),
				rs.getString("cnpj_dv"),
				rs.getString("identificador_matriz_filial"),
				rs.getString("nome_fantasia"),
				rs.getString("situacao_cadastral"),
				rs.getObject("data_situacao_cadastral", LocalDate.class),
				rs.getString("motivo_situacao_cadastral"),
				rs.getObject("data_inicio_atividade", LocalDate.class),
				rs.getString("pais"),
				rs.getString("cnae_fiscal_principal"),
				rs.getString("cnae_fiscal_secundaria"),
				rs.getString("tipo_logradouro"),
				rs.getString("logradouro"),
				rs.getString("numero"),
				rs.getString("complemento"),
				rs.getString("bairro"),
				rs.getString("cep"),
				rs.getString("uf"),
				rs.getString("municipio"),
				rs.getString("municipio_descricao"),
				rs.getString("nome_cidade_exterior"),
				rs.getString("ddd1"),
				rs.getString("telefone1"),
				rs.getString("ddd2"),
				rs.getString("telefone2"),
				rs.getString("ddd_fax"),
				rs.getString("fax"),
				rs.getString("correio_eletronico"),
				rs.getString("situacao_especial"),
				rs.getObject("data_situacao_especial", LocalDate.class),
				rs.getString("razao_social"),
				rs.getString("natureza_juridica"),
				rs.getString("natureza_juridica_descricao"),
				rs.getString("qualificacao_responsavel"),
				rs.getBigDecimal("capital_social"),
				rs.getString("porte"),
				rs.getString("ente_federativo"));
	}
}