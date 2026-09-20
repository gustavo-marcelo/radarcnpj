package ia.espalha.cnpj.shared.error;

public class EntidadeNaoEncontradaException extends RuntimeException {

	public EntidadeNaoEncontradaException(String mensagem) {
		super(mensagem);
	}
}