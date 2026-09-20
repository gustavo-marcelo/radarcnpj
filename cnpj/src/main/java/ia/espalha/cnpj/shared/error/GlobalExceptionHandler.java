package ia.espalha.cnpj.shared.error;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final URI CONFLITO = URI.create("https://api.radar-cnpj/problems/conflito");
	private static final URI NAO_ENCONTRADO = URI.create("https://api.radar-cnpj/problems/nao-encontrado");
	private static final URI VALIDACAO = URI.create("https://api.radar-cnpj/problems/validacao");
	private static final URI NAO_AUTENTICADO = URI.create("https://api.radar-cnpj/problems/nao-autenticado");
	private static final URI SEM_PERMISSAO = URI.create("https://api.radar-cnpj/problems/sem-permissao");
	private static final URI ERRO_INTERNO = URI.create("https://api.radar-cnpj/problems/erro-interno");

	@ExceptionHandler(CredenciaisInvalidasException.class)
	ResponseEntity<ProblemDetail> credenciaisInvalidas(CredenciaisInvalidasException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		pd.setTitle("Não autenticado");
		pd.setType(NAO_AUTENTICADO);
		pd.setDetail(ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> semPermissao(AccessDeniedException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		pd.setTitle("Sem permissão");
		pd.setType(SEM_PERMISSAO);
		pd.setDetail("Você não tem permissão para esta operação.");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
	}

	@ExceptionHandler(ConflitoException.class)
	ResponseEntity<ProblemDetail> conflito(ConflitoException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		pd.setTitle("Conflito de estado");
		pd.setType(CONFLITO);
		pd.setDetail(ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
	}

	@ExceptionHandler(EntidadeNaoEncontradaException.class)
	ResponseEntity<ProblemDetail> naoEncontrado(EntidadeNaoEncontradaException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setTitle("Não encontrado");
		pd.setType(NAO_ENCONTRADO);
		pd.setDetail(ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Erro de validação");
		pd.setType(VALIDACAO);
		pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
				.map(f -> Map.of("field", f.getField(), "message", f.getDefaultMessage()))
				.toList());
		return ResponseEntity.badRequest().body(pd);
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class })
	ResponseEntity<ProblemDetail> parametroInvalido(RuntimeException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Requisição inválida");
		pd.setType(VALIDACAO);
		pd.setDetail(ex.getMessage());
		return ResponseEntity.badRequest().body(pd);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> recursoInexistente() {
		var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setTitle("Não encontrado");
		pd.setType(NAO_ENCONTRADO);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
	}

	@ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ProblemDetail> metodoNaoSuportado(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
		pd.setTitle("Método não suportado");
		pd.setType(VALIDACAO);
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> inesperado(Exception ex) {
		log.error("Erro inesperado", ex);
		var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		pd.setTitle("Erro interno");
		pd.setType(ERRO_INTERNO);
		pd.setDetail("Ocorreu um erro inesperado. Tente novamente.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
	}
}