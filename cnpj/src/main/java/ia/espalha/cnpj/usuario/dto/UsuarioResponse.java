package ia.espalha.cnpj.usuario.dto;

import java.time.Instant;

import ia.espalha.cnpj.usuario.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String papel, boolean ativo, Instant criadoEm) {

	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(
				usuario.id(),
				usuario.nome(),
				usuario.email(),
				usuario.papel(),
				usuario.ativo(),
				usuario.criadoEm());
	}
}