package ia.espalha.cnpj.importacao;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ia.espalha.cnpj.importacao.dto.ConfigImportacao;

@Validated
@RestController
@RequestMapping("/api/config/importacao")
@PreAuthorize("hasRole('ADMIN')")
public class ConfigImportacaoController {

	private final ImportacaoService importacaoService;

	public ConfigImportacaoController(ImportacaoService importacaoService) {
		this.importacaoService = importacaoService;
	}

	@GetMapping
	public ConfigImportacao ler() {
		return importacaoService.lerConfig();
	}

	@PutMapping
	public ConfigImportacao salvar(@Valid @RequestBody ConfigImportacao config) {
		return importacaoService.salvarConfig(config);
	}
}