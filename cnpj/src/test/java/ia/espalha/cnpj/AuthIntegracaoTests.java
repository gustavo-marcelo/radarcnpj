package ia.espalha.cnpj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;

import tools.jackson.databind.JsonNode;

class AuthIntegracaoTests extends BaseIntegracaoTest {

	@Test
	void autenticaAdminComSucesso() {
		JsonNode corpo = rest().post()
				.uri("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.body(java.util.Map.of("email", "admin@exemplo.com", "senha", "admin123"))
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("token").asText()).isNotBlank();
		assertThat(corpo.at("/usuario/email").asText()).isEqualTo("admin@exemplo.com");
		assertThat(corpo.at("/usuario/papel").asText()).isEqualTo("ADMIN");
	}

	@Test
	void autenticaUsuarioComSucesso() {
		JsonNode corpo = rest().post()
				.uri("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.body(java.util.Map.of("email", "usuario@ex.com", "senha", "123456"))
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("token").asText()).isNotBlank();
		assertThat(corpo.at("/usuario/papel").asText()).isEqualTo("USER");
	}

	@Test
	void senhaIncorretaRetorna401() {
		assertThatThrownBy(() -> autenticar("admin@exemplo.com", "errada"))
				.isInstanceOf(HttpClientErrorException.Unauthorized.class);
	}

	@Test
	void meComTokenValidoRetornaUsuario() {
		String token = tokenAdmin();

		JsonNode corpo = autenticado(token).get()
				.uri("/api/auth/me")
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("email").asText()).isEqualTo("admin@exemplo.com");
	}

	@Test
	void endpointProtegidoSemTokenRetorna401() {
		assertThatThrownBy(() -> rest().get()
				.uri("/api/empresas")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.Unauthorized.class);
	}

	@Test
	void cadastroPublicoDesabilitadoRetorna403() {
		assertThatThrownBy(() -> rest().post()
				.uri("/api/auth/registrar")
				.contentType(MediaType.APPLICATION_JSON)
				.body(java.util.Map.of("nome", "Novo", "email", "novo@ex.com", "senha", "123456"))
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.Forbidden.class);
	}
}