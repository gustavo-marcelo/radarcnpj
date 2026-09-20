package ia.espalha.cnpj.auth.dto;

import ia.espalha.cnpj.usuario.dto.UsuarioResponse;

public record LoginResponse(String token, UsuarioResponse usuario) {
}