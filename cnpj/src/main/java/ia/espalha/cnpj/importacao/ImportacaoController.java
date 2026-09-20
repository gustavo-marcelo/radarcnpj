package ia.espalha.cnpj.importacao;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ia.espalha.cnpj.importacao.dto.StatusImportacao;

@RestController
@RequestMapping("/api/importacao")
@PreAuthorize("hasRole('ADMIN')")
public class ImportacaoController {

	private final ImportacaoService importacaoService;

	public ImportacaoController(ImportacaoService importacaoService) {
		this.importacaoService = importacaoService;
	}

	@PostMapping("/iniciar")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void iniciar() {
		importacaoService.iniciar();
	}

	@GetMapping("/status")
	public StatusImportacao status() {
		return importacaoService.status();
	}

	@PostMapping("/cancelar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelar() {
		importacaoService.cancelar();
	}
}