package ia.espalha.cnpj.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlterarUsuarioRequest(
		@NotBlank @Size(max = 150) String nome,
		@NotBlank @Pattern(regexp = "USER|ADMIN") String papel,
		@NotNull Boolean ativo) {
}