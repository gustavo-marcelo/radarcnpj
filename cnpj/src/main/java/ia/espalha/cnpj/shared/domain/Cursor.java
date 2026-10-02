package ia.espalha.cnpj.shared.domain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.util.StringUtils;

/**
 * Cursor de paginação por chave (keyset): guarda a última linha devolvida para
 * retomar a busca com {@code (chave) > (cursor)} em vez de {@code OFFSET}.
 *
 * <p>OFFSET é O(deslocamento): emEstablishment com 68M de linhas, chegar à página
 * 5000 de um CNAE com 1,6M de-estabelecimentos custava 2,9 s mesmo com índice,
 * porque o banco ainda percorre as 100 mil linhas anteriores. O seek usa o índice
 * como ponto de partida e o custo passa a depender apenas do tamanho da página.
 *
 * <p>Só funciona se a ordenação for a mesma chave do índice. Por isso a ordenação é
 * fixa em (cnpjBasico, cnpjOrdem), a mesma de {@code ix_estab_cnae_princ_cnpj}.
 */
public record Cursor(String cnpjBasico, String cnpjOrdem) {

	private static final String SEPARADOR = "|";

	public static Cursor of(String cnpjBasico, String cnpjOrdem) {
		return new Cursor(cnpjBasico, cnpjOrdem);
	}

	/**
	 * @return o cursor decodificado, ou {@code null} se ausente ou malformado.
	 *         Cursor inválido é tratado como ausência em vez de erro, para não
	 *         derrubar a listagem por um parâmetro manipulado.
	 */
	public static Cursor decode(String cursor) {
		if (!StringUtils.hasText(cursor)) {
			return null;
		}
		try {
			String bruto = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
			int separador = bruto.indexOf(SEPARADOR);
			if (separador <= 0 || separador == bruto.length() - 1) {
				return null;
			}
			return new Cursor(bruto.substring(0, separador), bruto.substring(separador + 1));
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public String encode() {
		return Base64.getUrlEncoder().withoutPadding()
				.encodeToString((cnpjBasico + SEPARADOR + cnpjOrdem).getBytes(StandardCharsets.UTF_8));
	}
}
