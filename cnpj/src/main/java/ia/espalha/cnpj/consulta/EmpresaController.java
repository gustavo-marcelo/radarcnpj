package ia.espalha.cnpj.consulta;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ia.espalha.cnpj.consulta.dto.EmpresaDetalheResponse;
import ia.espalha.cnpj.consulta.dto.EmpresaResponse;
import ia.espalha.cnpj.consulta.dto.EstabelecimentoResponse;
import ia.espalha.cnpj.shared.domain.PaginaResponse;
import ia.espalha.cnpj.shared.domain.Paginacao;
import ia.espalha.cnpj.shared.error.EntidadeNaoEncontradaException;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

	private final EmpresaConsultaRepository empresaRepository;
	private final EstabelecimentoConsultaRepository estabelecimentoRepository;

	public EmpresaController(EmpresaConsultaRepository empresaRepository,
			EstabelecimentoConsultaRepository estabelecimentoRepository) {
		this.empresaRepository = empresaRepository;
		this.estabelecimentoRepository = estabelecimentoRepository;
	}

	@GetMapping
	public PaginaResponse<EmpresaResponse> listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String razaoSocial,
			@RequestParam(required = false) String cnpjBasico,
			@RequestParam(required = false) String natureza) {
		int tamanho = Paginacao.tamanho(size);
		return empresaRepository.buscar(razaoSocial, cnpjBasico, natureza, Paginacao.pagina(page), tamanho);
	}

	@GetMapping("/{cnpjBasico}")
	public EmpresaDetalheResponse detalhe(@PathVariable String cnpjBasico) {
		String codigo = cnpjBasico.trim();
		EmpresaResponse empresa = empresaRepository.buscarPorCnpjBasico(codigo)
				.orElseThrow(() -> new EntidadeNaoEncontradaException("Empresa não encontrada."));
		return new EmpresaDetalheResponse(empresa, listarEstabelecimentos(codigo));
	}

	private List<EstabelecimentoResponse> listarEstabelecimentos(String cnpjBasico) {
		return estabelecimentoRepository.buscarPorEmpresa(cnpjBasico);
	}
}