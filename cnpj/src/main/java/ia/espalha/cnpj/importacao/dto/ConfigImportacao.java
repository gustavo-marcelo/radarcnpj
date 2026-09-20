package ia.espalha.cnpj.importacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConfigImportacao(
		@NotBlank @Size(max = 500) String dadosDir,
		@NotNull @Positive Integer threads,
		@NotNull Boolean truncateAntes) {
}