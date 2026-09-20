package ia.espalha.cnpj.consulta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ia.espalha.cnpj.consulta.dto.EstabelecimentoDetalheResponse;
import ia.espalha.cnpj.consulta.dto.EstabelecimentoResponse;
import ia.espalha.cnpj.shared.domain.PaginaResponse;
import ia.espalha.cnpj.shared.domain.Paginacao;
import ia.espalha.cnpj.shared.error.EntidadeNaoEncontradaException;

@RestController
@RequestMapping("/api/estabelecimentos")
public class EstabelecimentoController {

	private final EstabelecimentoConsultaRepository estabelecimentoRepository;

	public EstabelecimentoController(EstabelecimentoConsultaRepository estabelecimentoRepository) {
		this.estabelecimentoRepository = estabelecimentoRepository;
	}

	@GetMapping
	public PaginaResponse<EstabelecimentoResponse> listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String cnpjBasico,
			@RequestParam(required = false) String nomeFantasia,
			@RequestParam(required = false) String cnae,
			@RequestParam(required = false, defaultValue = "ambos") String tipoCnae,
			@RequestParam(required = false) String uf,
			@RequestParam(required = false) String municipio,
			@RequestParam(required = false) String situacao,
			@RequestParam(required = false) String matrizFilial) {
		int tamanho = Paginacao.tamanho(size);
		return estabelecimentoRepository.buscar(cnpjBasico, nomeFantasia, cnae, tipoCnae,
				uf, municipio, situacao, matrizFilial, Paginacao.pagina(page), tamanho);
	}

	@GetMapping("/{cnpjBasico}/{cnpjOrdem}/{cnpjDv}")
	public EstabelecimentoDetalheResponse detalhe(
			@PathVariable String cnpjBasico,
			@PathVariable String cnpjOrdem,
			@PathVariable String cnpjDv) {
		return estabelecimentoRepository.buscarDetalhe(cnpjBasico.trim(), cnpjOrdem.trim(), cnpjDv.trim())
				.orElseThrow(() -> new EntidadeNaoEncontradaException("Estabelecimento não encontrado."));
	}
}