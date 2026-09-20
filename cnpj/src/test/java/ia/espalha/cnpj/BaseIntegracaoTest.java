package ia.espalha.cnpj;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.import.enabled=false")
public abstract class BaseIntegracaoTest {

	@Autowired
	protected ObjectMapper mapper;

	@LocalServerPort
	protected int porta;

	protected RestClient rest() {
		return RestClient.create("http://localhost:" + porta);
	}

	protected String autenticar(String email, String senha) {
		JsonNode corpo = rest().post()
				.uri("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", email, "senha", senha))
				.retrieve()
				.body(JsonNode.class);
		return corpo.get("token").asText();
	}

	protected String tokenAdmin() {
		return autenticar("admin@exemplo.com", "admin123");
	}

	protected String tokenUsuario() {
		return autenticar("usuario@ex.com", "123456");
	}

	protected RestClient autenticado(String token) {
		return RestClient.builder()
				.baseUrl("http://localhost:" + porta)
				.defaultHeader("Authorization", "Bearer " + token)
				.build();
	}
}