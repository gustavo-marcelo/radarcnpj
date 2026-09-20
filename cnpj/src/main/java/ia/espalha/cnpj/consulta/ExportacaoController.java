package ia.espalha.cnpj.consulta;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exportacao")
public class ExportacaoController {

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final ExportacaoService exportacaoService;

	public ExportacaoController(ExportacaoService exportacaoService) {
		this.exportacaoService = exportacaoService;
	}

	@GetMapping("/empresas")
	public ResponseEntity<Resource> empresas(
			@RequestParam(required = false) String razaoSocial,
			@RequestParam(required = false) String cnpjBasico,
			@RequestParam(required = false) String natureza,
			@RequestParam(defaultValue = "10000") int limite) throws IOException {
		return resposta("empresas.xlsx",
				exportacaoService.exportarEmpresas(razaoSocial, cnpjBasico, natureza, Math.max(1, limite)));
	}

	@GetMapping("/estabelecimentos")
	public ResponseEntity<Resource> estabelecimentos(
			@RequestParam(required = false) String cnpjBasico,
			@RequestParam(required = false) String nomeFantasia,
			@RequestParam(required = false) String cnae,
			@RequestParam(required = false, defaultValue = "ambos") String tipoCnae,
			@RequestParam(required = false) String uf,
			@RequestParam(required = false) String municipio,
			@RequestParam(required = false) String situacao,
			@RequestParam(required = false) String matrizFilial,
			@RequestParam(defaultValue = "10000") int limite) throws IOException {
		return resposta("estabelecimentos.xlsx",
				exportacaoService.exportarEstabelecimentos(cnpjBasico, nomeFantasia, cnae,
						tipoCnae, uf, municipio, situacao, matrizFilial, Math.max(1, limite)));
	}

	@GetMapping("/cnaes")
	public ResponseEntity<Resource> cnaes(@RequestParam(required = false) String q) throws IOException {
		return resposta("cnaes.xlsx", exportacaoService.exportarCnaes(q));
	}

	private ResponseEntity<Resource> resposta(String arquivo, Path temporario) throws IOException {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(arquivo).build().toString())
				.contentType(XLSX)
				.contentLength(Files.size(temporario))
				.body(comLimpeza(temporario));
	}

	private Resource comLimpeza(Path temporario) {
		temporario.toFile().deleteOnExit();
		return new FileSystemResource(temporario) {
			@Override
			public InputStream getInputStream() throws IOException {
				InputStream in = super.getInputStream();
				return new FilterInputStream(in) {
					@Override
					public void close() throws IOException {
						super.close();
						Files.deleteIfExists(temporario);
					}
				};
			}
		};
	}
}