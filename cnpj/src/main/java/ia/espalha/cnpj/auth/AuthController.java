package ia.espalha.cnpj.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ia.espalha.cnpj.auth.dto.LoginRequest;
import ia.espalha.cnpj.auth.dto.LoginResponse;
import ia.espalha.cnpj.usuario.UsuarioService;
import ia.espalha.cnpj.usuario.dto.RegistrarRequest;
import ia.espalha.cnpj.usuario.dto.UsuarioResponse;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UsuarioService usuarioService;
	private final boolean cadastroPublico;

	public AuthController(UsuarioService usuarioService,
			@Value("${app.cadastro-publico:false}") boolean cadastroPublico) {
		this.usuarioService = usuarioService;
		this.cadastroPublico = cadastroPublico;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return usuarioService.autenticar(request.email(), request.senha());
	}

	@PostMapping("/registrar")
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioResponse registrar(@Valid @RequestBody RegistrarRequest request) {
		if (!cadastroPublico) {
			throw new AccessDeniedException("Cadastro público está desativado.");
		}
		return usuarioService.registrar(request);
	}

	@GetMapping("/me")
	public UsuarioResponse me(Authentication authentication) {
		return usuarioService.buscarPorEmailLogado(authentication.getName());
	}
}