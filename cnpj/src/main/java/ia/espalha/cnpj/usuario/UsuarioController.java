package ia.espalha.cnpj.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import ia.espalha.cnpj.shared.domain.PaginaResponse;
import ia.espalha.cnpj.usuario.dto.AlterarSenhaRequest;
import ia.espalha.cnpj.usuario.dto.AlterarUsuarioRequest;
import ia.espalha.cnpj.usuario.dto.CriarUsuarioRequest;
import ia.espalha.cnpj.usuario.dto.UsuarioResponse;

@Validated
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping
	public PaginaResponse<UsuarioResponse> listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return usuarioService.listar(page, size, q);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
		return usuarioService.criar(request);
	}

	@PutMapping("/{id}")
	public UsuarioResponse atualizar(@PathVariable long id, @Valid @RequestBody AlterarUsuarioRequest request) {
		return usuarioService.atualizar(id, request);
	}

	@PutMapping("/{id}/senha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void redefinirSenha(@PathVariable long id, @Valid @RequestBody AlterarSenhaRequest request) {
		usuarioService.redefinirSenha(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable long id, Authentication authentication) {
		usuarioService.remover(id, authentication.getName());
	}
}