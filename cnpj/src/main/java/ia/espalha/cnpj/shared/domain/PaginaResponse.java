package ia.espalha.cnpj.shared.domain;

import java.util.List;

public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
		String nextCursor) {

	public static <T> PaginaResponse<T> of(List<T> content, int page, int size, long totalElements) {
		return of(content, page, size, totalElements, null);
	}

	public static <T> PaginaResponse<T> of(List<T> content, int page, int size, long totalElements,
			String nextCursor) {
		int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
		return new PaginaResponse<>(List.copyOf(content), page, size, totalElements, totalPages, nextCursor);
	}
}
