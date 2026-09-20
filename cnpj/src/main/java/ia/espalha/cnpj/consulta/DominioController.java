package ia.espalha.cnpj.consulta;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ia.espalha.cnpj.consulta.dto.DominioResponse;

@RestController
@RequestMapping("/api/dominios")
public class DominioController {

	private final DominioConsultaRepository dominioRepository;

	public DominioController(DominioConsultaRepository dominioRepository) {
		this.dominioRepository = dominioRepository;
	}

	@GetMapping("/cnaes")
	public List<DominioResponse> cnaes(@RequestParam(required = false) String q) {
		return dominioRepository.buscar("cnae", q);
	}

	@GetMapping("/municipios")
	public List<DominioResponse> municipios(@RequestParam(required = false) String q) {
		return dominioRepository.buscar("municipio", q);
	}

	@GetMapping("/naturezas")
	public List<DominioResponse> naturezas(@RequestParam(required = false) String q) {
		return dominioRepository.buscar("natureza_juridica", q);
	}

	@GetMapping("/paises")
	public List<DominioResponse> paises(@RequestParam(required = false) String q) {
		return dominioRepository.buscar("pais", q);
	}

	@GetMapping("/qualificacoes")
	public List<DominioResponse> qualificacoes(@RequestParam(required = false) String q) {
		return dominioRepository.buscar("qualificacao", q);
	}

	@GetMapping("/motivos")
	public List<DominioResponse> motivos(@RequestParam(required = false) String q) {
		return dominioRepository.buscar("motivo_situacao_cadastral", q);
	}
}