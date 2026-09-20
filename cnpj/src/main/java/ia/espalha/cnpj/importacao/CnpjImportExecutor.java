package ia.espalha.cnpj.importacao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.sql.DataSource;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ia.espalha.cnpj.importacao.dto.ConfigImportacao;

@Component
public class CnpjImportExecutor {

	private static final Logger log = LoggerFactory.getLogger(CnpjImportExecutor.class);

	public interface Progresso {
		void novaTabela(String tabela);

		void novoArquivo(String arquivo);

		void linhas(long acrescimo);

		void mensagem(String mensagem);

		boolean cancelado();
	}

	public static final class ProgressoOcioso implements Progresso {
		@Override
		public void novaTabela(String tabela) {
		}

		@Override
		public void novoArquivo(String arquivo) {
		}

		@Override
		public void linhas(long acrescimo) {
		}

		@Override
		public void mensagem(String mensagem) {
		}

		@Override
		public boolean cancelado() {
			return false;
		}
	}

	private final DataSource dataSource;

	@Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/cnpj}")
	private String datasourceUrl;

	@Value("${spring.datasource.username:cnpj}")
	private String username;

	@Value("${spring.datasource.password:cnpj}")
	private String password;

	@Value("${app.import.database:cnpj}")
	private String databaseName;

	public CnpjImportExecutor(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void executar(ConfigImportacao config, Progresso progresso) throws Exception {
		Path dir = Paths.get(config.dadosDir()).toAbsolutePath().normalize();
		if (!Files.isDirectory(dir)) {
			throw new IOException("Diretorio de dados nao encontrado: " + dir);
		}
		ensureDatabase();
		createTables();
		if (config.truncateAntes()) {
			truncateTables();
		}
		if (progresso.cancelado()) {
			return;
		}
		ExecutorService executor = Executors.newFixedThreadPool(config.threads());
		try {
			long start = System.currentTimeMillis();
			for (CnpjTable table : CnpjTable.values()) {
				if (progresso.cancelado()) {
					break;
				}
				progresso.novaTabela(table.tableName());
				List<Path> zips = findZips(dir, table.zipPrefix());
				if (zips.isEmpty()) {
					log.warn("Nenhum arquivo {}*.zip encontrado em {}", table.zipPrefix(), dir);
					progresso.mensagem("Nenhum arquivo " + table.zipPrefix() + "*.zip encontrado");
					continue;
				}
				List<Future<Long>> futures = new ArrayList<>(zips.size());
				for (Path zip : zips) {
					if (progresso.cancelado()) {
						break;
					}
					futures.add(executor.submit(() -> importZip(zip, table, progresso)));
				}
				long total = 0;
				for (Future<Long> future : futures) {
					try {
						total += future.get();
					} catch (ExecutionException e) {
						throw new RuntimeException("Falha ao importar " + table.tableName(), e.getCause());
					}
				}
				progresso.linhas(total);
				long seconds = (System.currentTimeMillis() - start) / 1000;
				log.info("Tabela {} concluida com {} linhas em {}s.", table.tableName(), total, seconds);
				progresso.mensagem("Tabela " + table.tableName() + " concluída");
			}
		} finally {
			executor.shutdown();
		}
	}

	private long importZip(Path zip, CnpjTable table, Progresso progresso) throws Exception {
		Path work = extract(zip);
		try {
			return importExtractedFiles(work, table, progresso);
		} finally {
			deleteRecursively(work);
		}
	}

	private void ensureDatabase() throws SQLException {
		String adminUrl = adminUrl();
		log.info("Verificando existencia do banco de dados '{}' em {}.", databaseName, adminUrl);
		try (Connection conn = DriverManager.getConnection(adminUrl, username, password);
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'")) {
			if (rs.next()) {
				log.info("Banco de dados '{}' ja existe.", databaseName);
				return;
			}
		}
		try (Connection conn = DriverManager.getConnection(adminUrl, username, password);
				Statement st = conn.createStatement()) {
			st.executeUpdate("CREATE DATABASE " + databaseName);
			log.info("Banco de dados '{}' criado.", databaseName);
		}
	}

	private String adminUrl() {
		String prefix = "jdbc:postgresql://";
		int start = datasourceUrl.indexOf(prefix);
		if (start == -1) {
			return prefix + "localhost:5432/postgres";
		}
		String rest = datasourceUrl.substring(start + prefix.length());
		int slash = rest.lastIndexOf('/');
		return slash == -1 ? datasourceUrl : prefix + rest.substring(0, slash + 1) + "postgres";
	}

	private void createTables() throws SQLException {
		try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
			for (CnpjTable table : CnpjTable.values()) {
				StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
						.append(table.tableName()).append(" (");
				boolean first = true;
				for (CnpjColumn col : table.columns()) {
					if (!first) {
						ddl.append(", ");
					}
					first = false;
					ddl.append(col.name()).append(' ').append(col.sqlType());
				}
				ddl.append(')');
				st.executeUpdate(ddl.toString());
				log.info("Tabela {} criada/verificada.", table.tableName());
			}
		}
	}

	private void truncateTables() throws SQLException {
		try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
			for (CnpjTable table : CnpjTable.values()) {
				st.executeUpdate("TRUNCATE TABLE " + table.tableName());
			}
			log.info("Tabelas truncadas antes da importacao.");
		}
	}

	private static List<Path> findZips(Path dir, String prefix) throws IOException {
		List<Path> zips = new ArrayList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
			for (Path p : ds) {
				String name = p.getFileName().toString();
				if (name.toLowerCase().endsWith(".zip") && name.regionMatches(0, prefix, 0, prefix.length())) {
					zips.add(p);
				}
			}
		}
		zips.sort(Comparator.comparing(p -> p.getFileName().toString()));
		return zips;
	}

	private static Path extract(Path zip) throws IOException {
		Path work = Files.createTempDirectory("cnpj-import");
		log.info("Descompactando {} em {}.", zip.getFileName(), work);
		try (ZipFile zipFile = new ZipFile(zip.toFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				Path out = work.resolve(Paths.get(entry.getName()).getFileName().toString());
				try (InputStream in = zipFile.getInputStream(entry);
						OutputStream outFile = Files.newOutputStream(out)) {
					in.transferTo(outFile);
				}
				log.info("Arquivo descompactado: {}", out.getFileName());
			}
		}
		return work;
	}

	private long importExtractedFiles(Path work, CnpjTable table, Progresso progresso) throws IOException, SQLException {
		long total = 0;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(work)) {
			for (Path file : ds) {
				if (Files.isRegularFile(file)) {
					if (progresso.cancelado()) {
						break;
					}
					total += copyCsv(file, table, progresso);
				}
			}
		}
		return total;
	}

	private long copyCsv(Path file, CnpjTable table, Progresso progresso) throws SQLException {
		String sql = "COPY " + table.tableName()
				+ " FROM STDIN WITH (FORMAT csv, DELIMITER ';', NULL '', HEADER false)";
		log.info("Importando {} na tabela {}.", file.getFileName(), table.tableName());
		progresso.novoArquivo(file.getFileName().toString());
		try (Connection conn = dataSource.getConnection()) {
			BaseConnection base = conn.unwrap(BaseConnection.class);
			CopyManager copy = base.getCopyAPI();
			try (InputStream in = Files.newInputStream(file);
					Reader reader = new InputStreamReader(in, StandardCharsets.ISO_8859_1);
					BufferedReader buffered = new BufferedReader(reader, 1 << 20);
					CsvTransformReader transform = new CsvTransformReader(buffered, table)) {
				long rows = copy.copyIn(sql, transform);
				log.info("{} linhas importadas de {} em {}.", rows, file.getFileName(), table.tableName());
				return rows;
			} catch (IOException e) {
				throw new SQLException("Falha ao ler " + file + " para " + table.tableName(), e);
			}
		}
	}

	private static void deleteRecursively(Path dir) {
		try (var paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder())
					.forEach(path -> {
						try {
							Files.deleteIfExists(path);
						} catch (IOException e) {
							log.warn("Nao foi possivel apagar {}", path, e);
						}
					});
		} catch (IOException e) {
			log.warn("Nao foi possivel limpar o diretorio {}", dir, e);
		}
	}
}