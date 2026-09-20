package ia.espalha.cnpj.usuario;

import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ia.espalha.cnpj.auth.JwtService;
import ia.espalha.cnpj.auth.dto.LoginResponse;
import ia.espalha.cnpj.shared.domain.PaginaResponse;
import ia.espalha.cnpj.shared.error.ConflitoException;
import ia.espalha.cnpj.shared.error.CredenciaisInvalidasException;
import ia.espalha.cnpj.shared.error.EntidadeNaoEncontradaException;
import ia.espalha.cnpj.usuario.dto.AlterarSenhaRequest;
import ia.espalha.cnpj.usuario.dto.AlterarUsuarioRequest;
import ia.espalha.cnpj.usuario.dto.CriarUsuarioRequest;
import ia.espalha.cnpj.usuario.dto.RegistrarRequest;
import ia.espalha.cnpj.usuario.dto.UsuarioResponse;

@Service
public class UsuarioService {

	private static final int TAMANHO_MAXIMO = 100;

	private final UsuarioRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResponse autenticar(String email, String senha) {
		Usuario usuario = repository.findByEmail(normalizarEmail(email))
				.filter(Usuario::ativo)
				.orElseThrow(CredenciaisInvalidasException::new);
		if (!passwordEncoder.matches(senha, usuario.senhaHash())) {
			throw new CredenciaisInvalidasException();
		}
		return new LoginResponse(jwtService.gerarToken(usuario), UsuarioResponse.from(usuario));
	}

	public UsuarioResponse registrar(RegistrarRequest request) {
		garantirEmailDisponivel(request.email());
		long id = repository.insert(new Usuario(
				null, request.nome(), normalizarEmail(request.email()),
				passwordEncoder.encode(request.senha()), "USER", true, null, null));
		return UsuarioResponse.from(buscar(id));
	}

	public UsuarioResponse criar(CriarUsuarioRequest request) {
		garantirEmailDisponivel(request.email());
		long id = repository.insert(new Usuario(
				null, request.nome(), normalizarEmail(request.email()),
				passwordEncoder.encode(request.senha()), request.papel(), true, null, null));
		return UsuarioResponse.from(buscar(id));
	}

	public PaginaResponse<UsuarioResponse> listar(int page, int size, String filtro) {
		int tamanho = Math.min(Math.max(size, 1), TAMANHO_MAXIMO);
		int offset = Math.max(page, 0) * tamanho;
		long total = repository.countFiltrado(filtro);
		var content = repository.list(tamanho, offset, filtro).stream()
				.map(UsuarioResponse::from)
				.toList();
		return PaginaResponse.of(content, Math.max(page, 0), tamanho, total);
	}

	public UsuarioResponse atualizar(long id, AlterarUsuarioRequest request) {
		Usuario usuario = buscar(id);
		repository.update(new Usuario(
				usuario.id(), request.nome(), usuario.email(), usuario.senhaHash(),
				request.papel(), request.ativo(), usuario.criadoEm(), usuario.atualizadoEm()));
		return UsuarioResponse.from(buscar(id));
	}

	public void redefinirSenha(long id, AlterarSenhaRequest request) {
		buscar(id);
		repository.updateSenha(id, passwordEncoder.encode(request.novaSenha()));
	}

	public void remover(long id, String emailLogado) {
		Usuario usuario = buscar(id);
		if (usuario.email().equals(normalizarEmail(emailLogado))) {
			throw new ConflitoException("Não é possível remover o próprio usuário.");
		}
		repository.delete(id);
	}

	public UsuarioResponse buscarPorEmailLogado(String email) {
		return repository.findByEmail(normalizarEmail(email))
				.map(UsuarioResponse::from)
				.orElseThrow(() -> new AccessDeniedException("Usuário não encontrado."));
	}

	public Usuario buscar(String email) {
		return repository.findByEmail(normalizarEmail(email))
				.orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado."));
	}

	private Usuario buscar(long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado."));
	}

	private void garantirEmailDisponivel(String email) {
		if (repository.existsByEmail(normalizarEmail(email))) {
			throw new ConflitoException("E-mail já cadastrado.");
		}
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}