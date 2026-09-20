package ia.espalha.cnpj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import tools.jackson.databind.JsonNode;

class AutorizacaoIntegracaoTests extends BaseIntegracaoTest {

	@Test
	void usuarioComumNaoAcessaUsuarios() {
		assertThatThrownBy(() -> autenticado(tokenUsuario()).get()
				.uri("/api/usuarios")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.Forbidden.class);
	}

	@Test
	void usuarioComumNaoAcessaConfigImportacao() {
		assertThatThrownBy(() -> autenticado(tokenUsuario()).get()
				.uri("/api/config/importacao")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.Forbidden.class);
	}

	@Test
	void usuarioComumNaoIniciaImportacao() {
		assertThatThrownBy(() -> autenticado(tokenUsuario()).post()
				.uri("/api/importacao/iniciar")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.Forbidden.class);
	}

	@Test
	void adminAcessaUsuarios() {
		JsonNode corpo = autenticado(tokenAdmin()).get()
				.uri("/api/usuarios")
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("content").isArray()).isTrue();
	}

	@Test
	void adminAcessaConfigImportacao() {
		JsonNode corpo = autenticado(tokenAdmin()).get()
				.uri("/api/config/importacao")
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("dadosDir").isTextual()).isTrue();
	}

	@Test
	void usuarioComumAcessaConsulta() {
		JsonNode corpo = autenticado(tokenUsuario()).get()
				.uri("/api/empresas")
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("content").isArray()).isTrue();
	}
}