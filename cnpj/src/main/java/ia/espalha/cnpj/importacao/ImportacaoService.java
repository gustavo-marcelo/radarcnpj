package ia.espalha.cnpj.importacao;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ia.espalha.cnpj.importacao.CnpjImportExecutor.Progresso;
import ia.espalha.cnpj.importacao.dto.ConfigImportacao;
import ia.espalha.cnpj.importacao.dto.StatusImportacao;
import ia.espalha.cnpj.shared.error.ConflitoException;

@Service
public class ImportacaoService implements Progresso {

	private static final Logger log = LoggerFactory.getLogger(ImportacaoService.class);

	private final CnpjImportExecutor executor;
	private final ConfigImportacaoRepository configRepository;

	private final String dadosDirPadrao;
	private final int threadsPadrao;
	private final boolean truncatePadrao;

	private final ExecutorService threadImportacao = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = Thread.ofVirtual().unstarted(runnable);
		thread.setName("importacao");
		thread.setUncaughtExceptionHandler((t, e) -> log.error("Falha na thread de importacao", e));
		return thread;
	});

	private volatile EstadoImportacao estado = EstadoImportacao.IDLE;
	private volatile String tabelaAtual;
	private volatile String arquivoAtual;
	private final AtomicLong linhasImportadas = new AtomicLong();
	private volatile Instant iniciadoEm;
	private volatile Instant finalizadoEm;
	private volatile String mensagem;
	private volatile boolean cancelarSolicitado;

	public ImportacaoService(
			CnpjImportExecutor executor,
			ConfigImportacaoRepository configRepository,
			@Value("${app.dados.dir:dados}") String dadosDirPadrao,
			@Value("${app.import.threads:4}") int threadsPadrao,
			@Value("${app.import.truncate-before:true}") boolean truncatePadrao) {
		this.executor = executor;
		this.configRepository = configRepository;
		this.dadosDirPadrao = dadosDirPadrao;
		this.threadsPadrao = threadsPadrao;
		this.truncatePadrao = truncatePadrao;
	}

	private static Thread.Builder.OfVirtual executorThread() {
		return Thread.ofVirtual()
				.name("importacao-")
				.uncaughtExceptionHandler((t, e) -> log.error("Falha na thread de importacao", e));
	}

	public void iniciar() {
		synchronized (this) {
			if (estado == EstadoImportacao.RUNNING) {
				throw new ConflitoException("Já existe uma importação em andamento.");
			}
			estado = EstadoImportacao.RUNNING;
			tabelaAtual = null;
			arquivoAtual = null;
			linhasImportadas.set(0);
			mensagem = null;
			iniciadoEm = Instant.now();
			finalizadoEm = null;
			cancelarSolicitado = false;
		}
		ConfigImportacao config = lerConfig();
		threadImportacao.execute(() -> executarEmSegundoPlano(config));
	}

	private void executarEmSegundoPlano(ConfigImportacao config) {
		try {
			executor.executar(config, this);
			synchronized (this) {
				estado = cancelarSolicitado
						? EstadoImportacao.CANCELED
						: EstadoImportacao.DONE;
				finalizadoEm = Instant.now();
			}
			log.info("Importacao finalizada com estado {}", estado);
		} catch (Exception e) {
			synchronized (this) {
				estado = EstadoImportacao.ERROR;
				finalizadoEm = Instant.now();
				mensagem = e.getMessage();
			}
			log.error("Falha na importacao dos dados CNPJ.", e);
		}
	}

	public void cancelar() {
		if (estado == EstadoImportacao.RUNNING) {
			cancelarSolicitado = true;
			mensagem = "Cancelamento solicitado; aguardando conclusão do arquivo atual.";
		}
	}

	public StatusImportacao status() {
		return new StatusImportacao(
				estado,
				tabelaAtual,
				arquivoAtual,
				linhasImportadas.get(),
				iniciadoEm,
				finalizadoEm,
				mensagem);
	}

	public ConfigImportacao lerConfig() {
		return configRepository.find()
				.orElseGet(() -> new ConfigImportacao(dadosDirPadrao, threadsPadrao, truncatePadrao));
	}

	public ConfigImportacao salvarConfig(ConfigImportacao config) {
		configRepository.upsert(config);
		return lerConfig();
	}

	@Override
	public void novaTabela(String tabela) {
		this.tabelaAtual = tabela;
	}

	@Override
	public void novoArquivo(String arquivo) {
		this.arquivoAtual = arquivo;
	}

	@Override
	public void linhas(long acrescimo) {
		this.linhasImportadas.addAndGet(acrescimo);
	}

	@Override
	public void mensagem(String mensagem) {
		this.mensagem = mensagem;
	}

	@Override
	public boolean cancelado() {
		return cancelarSolicitado;
	}
}