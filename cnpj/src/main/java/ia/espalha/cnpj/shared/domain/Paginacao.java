package ia.espalha.cnpj.shared.domain;

public final class Paginacao {

	public static final int PADRAO = 20;
	public static final int MAXIMO = 100;

	private Paginacao() {
	}

	public static int tamanho(int size) {
		return Math.min(Math.max(size, 1), MAXIMO);
	}

	public static int pagina(int page) {
		return Math.max(page, 0);
	}
}