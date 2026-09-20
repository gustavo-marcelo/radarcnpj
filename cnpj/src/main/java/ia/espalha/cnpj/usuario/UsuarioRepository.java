package ia.espalha.cnpj.usuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioRepository {

	private static final String COLUNAS = "id, nome, email, senha_hash, papel, ativo, criado_em, atualizado_em";

	private final JdbcTemplate jdbc;

	public UsuarioRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Usuario> findByEmail(String email) {
		List<Usuario> resultados = jdbc.query(
				"SELECT " + COLUNAS + " FROM usuario WHERE email = ?",
				(rs, rowNum) -> toUsuario(rs),
				email);
		return resultados.stream().findFirst();
	}

	public Optional<Usuario> findById(long id) {
		List<Usuario> resultados = jdbc.query(
				"SELECT " + COLUNAS + " FROM usuario WHERE id = ?",
				(rs, rowNum) -> toUsuario(rs),
				id);
		return resultados.stream().findFirst();
	}

	public boolean existsByEmail(String email) {
		Integer count = jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Integer.class, email);
		return count != null && count > 0;
	}

	public long count() {
		Long count = jdbc.queryForObject("SELECT count(*) FROM usuario", Long.class);
		return count == null ? 0 : count;
	}

	public Long insert(Usuario usuario) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nome, email, senha_hash, papel, ativo)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, usuario.nome(), usuario.email(), usuario.senhaHash(), usuario.papel(), usuario.ativo());
	}

	public void update(Usuario usuario) {
		jdbc.update("""
				UPDATE usuario
				SET nome = ?, papel = ?, ativo = ?, atualizado_em = now()
				WHERE id = ?
				""", usuario.nome(), usuario.papel(), usuario.ativo(), usuario.id());
	}

	public void updateSenha(long id, String senhaHash) {
		jdbc.update("UPDATE usuario SET senha_hash = ?, atualizado_em = now() WHERE id = ?", senhaHash, id);
	}

	public void delete(long id) {
		jdbc.update("DELETE FROM usuario WHERE id = ?", id);
	}

	public List<Usuario> list(int limit, int offset, String filtro) {
		if (filtro == null || filtro.isBlank()) {
			return jdbc.query(
					"SELECT " + COLUNAS + " FROM usuario ORDER BY nome LIMIT ? OFFSET ?",
					(rs, rowNum) -> toUsuario(rs),
					limit, offset);
		}
		String like = "%" + filtro.trim() + "%";
		return jdbc.query(
				"SELECT " + COLUNAS
						+ " FROM usuario WHERE nome ILIKE ? OR email ILIKE ? ORDER BY nome LIMIT ? OFFSET ?",
				(rs, rowNum) -> toUsuario(rs),
				like, like, limit, offset);
	}

	public long countFiltrado(String filtro) {
		if (filtro == null || filtro.isBlank()) {
			return count();
		}
		String like = "%" + filtro.trim() + "%";
		Long count = jdbc.queryForObject(
				"SELECT count(*) FROM usuario WHERE nome ILIKE ? OR email ILIKE ?",
				Long.class, like, like);
		return count == null ? 0 : count;
	}

	private static Usuario toUsuario(ResultSet rs) throws SQLException {
		var criadoEm = rs.getTimestamp("criado_em");
		var atualizadoEm = rs.getTimestamp("atualizado_em");
		return new Usuario(
				rs.getLong("id"),
				rs.getString("nome"),
				rs.getString("email"),
				rs.getString("senha_hash"),
				rs.getString("papel"),
				rs.getBoolean("ativo"),
				criadoEm == null ? null : criadoEm.toInstant(),
				atualizadoEm == null ? null : atualizadoEm.toInstant());
	}
}