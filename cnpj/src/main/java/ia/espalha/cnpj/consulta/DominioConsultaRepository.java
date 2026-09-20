package ia.espalha.cnpj.consulta;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import ia.espalha.cnpj.consulta.dto.DominioResponse;

@Repository
public class DominioConsultaRepository {

	private static final int LIMITE_PADRAO = 100;

	private final JdbcTemplate jdbc;

	public DominioConsultaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<DominioResponse> buscar(String tabela, String q) {
		if (!StringUtils.hasText(q)) {
			return jdbc.query(
					"SELECT codigo, descricao FROM " + tabela + " ORDER BY descricao LIMIT ?",
					(rs, rowNum) -> new DominioResponse(rs.getString("codigo"), rs.getString("descricao")),
					LIMITE_PADRAO);
		}
		String like = "%" + q.trim() + "%";
		return jdbc.query(
				"SELECT codigo, descricao FROM " + tabela
						+ " WHERE codigo ILIKE ? OR descricao ILIKE ? ORDER BY descricao LIMIT ?",
				(rs, rowNum) -> new DominioResponse(rs.getString("codigo"), rs.getString("descricao")),
				like, like, LIMITE_PADRAO);
	}

	public List<DominioResponse> buscarParaExportacao(String tabela, String q) {
		if (!StringUtils.hasText(q)) {
			return jdbc.query(
					"SELECT codigo, descricao FROM " + tabela + " ORDER BY descricao",
					(rs, rowNum) -> new DominioResponse(rs.getString("codigo"), rs.getString("descricao")));
		}
		String like = "%" + q.trim() + "%";
		return jdbc.query(
				"SELECT codigo, descricao FROM " + tabela
						+ " WHERE codigo ILIKE ? OR descricao ILIKE ? ORDER BY descricao",
				(rs, rowNum) -> new DominioResponse(rs.getString("codigo"), rs.getString("descricao")),
				like, like);
	}
}