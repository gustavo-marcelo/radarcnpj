package ia.espalha.cnpj.usuario;

import java.time.Instant;

public record Usuario(
		Long id,
		String nome,
		String email,
		String senhaHash,
		String papel,
		boolean ativo,
		Instant criadoEm,
		Instant atualizadoEm) {

	public boolean isAdmin() {
		return "ADMIN".equals(papel);
	}
}