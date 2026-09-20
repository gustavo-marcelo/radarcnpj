package ia.espalha.cnpj.importacao;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ia.espalha.cnpj.importacao.dto.ConfigImportacao;

@Repository
public class ConfigImportacaoRepository {

	private static final String COLUNAS = "dados_dir, threads, truncate_antes";

	private final JdbcTemplate jdbc;

	public ConfigImportacaoRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<ConfigImportacao> find() {
		return jdbc.query("SELECT " + COLUNAS + " FROM configuracao_importacao WHERE id = 1",
				(rs, rowNum) -> new ConfigImportacao(
						rs.getString("dados_dir"),
						rs.getInt("threads"),
						rs.getBoolean("truncate_antes")))
				.stream()
				.findFirst();
	}

	public void upsert(ConfigImportacao config) {
		jdbc.update("""
				INSERT INTO configuracao_importacao (id, dados_dir, threads, truncate_antes, atualizado_em)
				VALUES (1, ?, ?, ?, now())
				ON CONFLICT (id) DO UPDATE
				SET dados_dir = EXCLUDED.dados_dir,
				    threads = EXCLUDED.threads,
				    truncate_antes = EXCLUDED.truncate_antes,
				    atualizado_em = now()
				""", config.dadosDir(), config.threads(), config.truncateAntes());
	}
}