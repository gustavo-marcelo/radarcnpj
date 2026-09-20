package ia.espalha.cnpj.importacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CnpjImportBootRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CnpjImportBootRunner.class);

	private final ImportacaoService importacaoService;
	private final boolean enabled;

	public CnpjImportBootRunner(ImportacaoService importacaoService,
			@Value("${app.import.enabled:false}") boolean enabled) {
		this.importacaoService = importacaoService;
		this.enabled = enabled;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!enabled) {
			log.info("Importacao no boot desativada (app.import.enabled=false).");
			return;
		}
		log.info("Disparando importacao no boot.");
		importacaoService.iniciar();
	}
}