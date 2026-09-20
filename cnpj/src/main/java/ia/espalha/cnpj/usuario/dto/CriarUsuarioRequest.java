package ia.espalha.cnpj.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
		@NotBlank @Size(max = 150) String nome,
		@NotBlank @Email @Size(max = 200) String email,
		@NotBlank @Size(min = 6, max = 72) String senha,
		@NotBlank @Pattern(regexp = "USER|ADMIN") String papel) {
}