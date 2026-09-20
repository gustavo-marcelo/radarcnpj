package ia.espalha.cnpj.consulta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import ia.espalha.cnpj.consulta.dto.EmpresaResponse;
import ia.espalha.cnpj.shared.domain.PaginaResponse;

@Repository
public class EmpresaConsultaRepository {

	private static final String COLUNAS = """
			cnpj_basico, razao_social, natureza_juridica, qualificacao_responsavel,
			capital_social, porte, ente_federativo""";

	private final JdbcTemplate jdbc;

	public EmpresaConsultaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<EmpresaResponse> buscarPorCnpjBasico(String cnpjBasico) {
		return jdbc.query(
				"SELECT " + COLUNAS + " FROM empresa WHERE cnpj_basico = ?",
				(rs, rowNum) -> toEmpresa(rs),
				cnpjBasico).stream().findFirst();
	}

	public PaginaResponse<EmpresaResponse> buscar(String razaoSocial, String cnpjBasico, String natureza,
			int page, int size) {
		Filtros f = montarFiltros(razaoSocial, cnpjBasico, natureza);
		long total = jdbc.queryForObject("SELECT count(*) FROM empresa" + f.where(), Long.class, f.params().toArray());

		List<Object> params = new ArrayList<>(f.params());
		params.add(size);
		params.add((long) page * size);
		List<EmpresaResponse> content = jdbc.query(
				"SELECT " + COLUNAS + " FROM empresa" + f.where()
						+ " ORDER BY razao_social LIMIT ? OFFSET ?",
				(rs, rowNum) -> toEmpresa(rs),
				params.toArray());
		return PaginaResponse.of(content, page, size, total);
	}

	public List<EmpresaResponse> exportar(String razaoSocial, String cnpjBasico, String natureza, int limite) {
		Filtros f = montarFiltros(razaoSocial, cnpjBasico, natureza);
		List<Object> params = new ArrayList<>(f.params());
		params.add(limite);
		return jdbc.query(
				"SELECT " + COLUNAS + " FROM empresa" + f.where()
						+ " ORDER BY cnpj_basico LIMIT ?",
				(rs, rowNum) -> toEmpresa(rs),
				params.toArray());
	}

	private record Filtros(String where, List<Object> params) {
	}

	private static Filtros montarFiltros(String razaoSocial, String cnpjBasico, String natureza) {
		StringBuilder where = new StringBuilder(" WHERE TRUE");
		List<Object> params = new ArrayList<>(4);
		if (StringUtils.hasText(razaoSocial)) {
			where.append(" AND razao_social ILIKE ?");
			params.add("%" + razaoSocial.trim() + "%");
		}
		if (StringUtils.hasText(cnpjBasico)) {
			where.append(" AND cnpj_basico = ?");
			params.add(cnpjBasico.trim());
		}
		if (StringUtils.hasText(natureza)) {
			where.append(" AND natureza_juridica = ?");
			params.add(natureza.trim());
		}
		return new Filtros(where.toString(), params);
	}

	private static EmpresaResponse toEmpresa(java.sql.ResultSet rs) throws java.sql.SQLException {
		BigDecimal capital = rs.getBigDecimal("capital_social");
		return new EmpresaResponse(
				rs.getString("cnpj_basico"),
				rs.getString("razao_social"),
				rs.getString("natureza_juridica"),
				rs.getString("qualificacao_responsavel"),
				capital,
				rs.getString("porte"),
				rs.getString("ente_federativo"));
	}
}