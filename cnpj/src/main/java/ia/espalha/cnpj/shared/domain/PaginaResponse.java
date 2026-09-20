package ia.espalha.cnpj.shared.domain;

import java.util.List;

public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PaginaResponse<T> of(List<T> content, int page, int size, long totalElements) {
		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
		return new PaginaResponse<>(List.copyOf(content), page, size, totalElements, totalPages);
	}
}