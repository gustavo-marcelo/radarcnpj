package ia.espalha.cnpj.consulta;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import ia.espalha.cnpj.consulta.dto.DominioResponse;
import ia.espalha.cnpj.consulta.dto.EmpresaResponse;
import ia.espalha.cnpj.consulta.dto.EstabelecimentoResponse;

@Service
public class ExportacaoService {

	private static final String[] TITULOS_EMPRESAS = {
			"CNPJ Básico", "Razão Social", "Natureza Jurídica", "Qualificação do Responsável",
			"Capital Social", "Porte", "Ente Federativo" };

	private static final String[] TITULOS_ESTABELECIMENTOS = {
			"CNPJ Completo", "CNPJ Básico", "Ordem", "DV", "Matriz/Filial", "Razão Social",
			"Nome Fantasia", "Situação", "Data Situação", "CNAE Principal", "CNAE Secundária",
			"Logradouro", "Número", "Complemento", "Bairro", "CEP", "UF", "Município",
			"Município (descrição)", "Telefone", "E-mail", "Porte" };

	private static final String[] TITULOS_DOMINIOS = { "Código", "Descrição" };

	private final EmpresaConsultaRepository empresaRepository;
	private final EstabelecimentoConsultaRepository estabelecimentoRepository;
	private final DominioConsultaRepository dominioRepository;

	public ExportacaoService(EmpresaConsultaRepository empresaRepository,
			EstabelecimentoConsultaRepository estabelecimentoRepository,
			DominioConsultaRepository dominioRepository) {
		this.empresaRepository = empresaRepository;
		this.estabelecimentoRepository = estabelecimentoRepository;
		this.dominioRepository = dominioRepository;
	}

	public Path exportarEmpresas(String razaoSocial, String cnpjBasico, String natureza,
			int limite) throws IOException {
		return gravarArquivo("empresas", "Empresas", TITULOS_EMPRESAS,
				empresaRepository.exportar(razaoSocial, cnpjBasico, natureza, limite),
				(row, e) -> {
					texto(row, 0, e.cnpjBasico());
					texto(row, 1, e.razaoSocial());
					texto(row, 2, e.naturezaJuridica());
					texto(row, 3, e.qualificacaoResponsavel());
					numero(row, 4, e.capitalSocial());
					texto(row, 5, e.porte());
					texto(row, 6, e.enteFederativo());
				});
	}

	public Path exportarEstabelecimentos(String cnpjBasico, String nomeFantasia, String cnae,
			String tipoCnae, String uf, String municipio, String situacao, String matrizFilial,
			int limite) throws IOException {
		return gravarArquivo("estabelecimentos", "Estabelecimentos", TITULOS_ESTABELECIMENTOS,
				estabelecimentoRepository.exportar(cnpjBasico, nomeFantasia, cnae, tipoCnae,
						uf, municipio, situacao, matrizFilial, limite),
				(row, e) -> {
					texto(row, 0, e.cnpjCompleto());
					texto(row, 1, e.cnpjBasico());
					texto(row, 2, e.cnpjOrdem());
					texto(row, 3, e.cnpjDv());
					texto(row, 4, e.identificadorMatrizFilial());
					texto(row, 5, e.razaoSocial());
					texto(row, 6, e.nomeFantasia());
					texto(row, 7, e.situacaoCadastral());
					data(row, 8, e.dataSituacaoCadastral());
					texto(row, 9, e.cnaeFiscalPrincipal());
					texto(row, 10, e.cnaeFiscalSecundaria());
					texto(row, 11, e.logradouro());
					texto(row, 12, e.numero());
					texto(row, 13, e.complemento());
					texto(row, 14, e.bairro());
					texto(row, 15, e.cep());
					texto(row, 16, e.uf());
					texto(row, 17, e.municipio());
					texto(row, 18, e.municipioDescricao());
					texto(row, 19, telefone(e.ddd1(), e.telefone1()));
					texto(row, 20, e.correioEletronico());
					texto(row, 21, e.porte());
				});
	}

	public Path exportarCnaes(String q) throws IOException {
		return gravarArquivo("cnaes", "CNAE", TITULOS_DOMINIOS,
				dominioRepository.buscarParaExportacao("cnae", q),
				(row, d) -> {
					texto(row, 0, d.codigo());
					texto(row, 1, d.descricao());
				});
	}

	private <T> Path gravarArquivo(String prefixo, String nomeAba, String[] titulos, List<T> linhas,
			BiConsumer<Row, T> preencher) throws IOException {
		Path arquivo = Files.createTempFile("cnpj-" + prefixo + "-", ".xlsx");
		try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
				OutputStream out = Files.newOutputStream(arquivo)) {
			Sheet sheet = wb.createSheet(nomeAba.substring(0, Math.min(nomeAba.length(), 31)));
			CellStyle estilocabecalho = estiloCabecalho(wb);

			Row cabecalho = sheet.createRow(0);
			for (int i = 0; i < titulos.length; i++) {
				Cell celula = cabecalho.createCell(i);
				celula.setCellValue(titulos[i]);
				celula.setCellStyle(estilocabecalho);
			}
			int indice = 1;
			for (T linha : linhas) {
				preencher.accept(sheet.createRow(indice++), linha);
			}
			for (int i = 0; i < titulos.length; i++) {
				sheet.setColumnWidth(i, 20 * 256);
			}
			wb.write(out);
			wb.dispose();
		}
		return arquivo;
	}

	private CellStyle estiloCabecalho(SXSSFWorkbook wb) {
		CellStyle estilo = wb.createCellStyle();
		estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font fonte = wb.createFont();
		fonte.setBold(true);
		estilo.setFont(fonte);
		return estilo;
	}

	private static void texto(Row row, int indice, String valor) {
		if (valor != null) {
			row.createCell(indice).setCellValue(valor);
		}
	}

	private static void numero(Row row, int indice, BigDecimal valor) {
		if (valor != null) {
			row.createCell(indice).setCellValue(valor.doubleValue());
		}
	}

	private static void data(Row row, int indice, java.time.LocalDate valor) {
		if (valor != null) {
			row.createCell(indice).setCellValue(valor);
		}
	}

	private static String telefone(String ddd, String numero) {
		if (numero == null || numero.isBlank()) {
			return null;
		}
		return ddd == null || ddd.isBlank() ? numero : "(" + ddd + ") " + numero;
	}
}