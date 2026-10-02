package ia.espalha.cnpj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import tools.jackson.databind.JsonNode;

class ConsultaIntegracaoTests extends BaseIntegracaoTest {

	private JsonNode empresas(String token, String param, String valor) {
		return autenticado(token).get()
				.uri(b -> b.path("/api/empresas").queryParam(param, valor).build())
				.retrieve()
				.body(JsonNode.class);
	}

	private JsonNode estabelecimentos(String token, String param, String valor) {
		return autenticado(token).get()
				.uri(b -> b.path("/api/estabelecimentos").queryParam(param, valor).build())
				.retrieve()
				.body(JsonNode.class);
	}

	private JsonNode estabelecimentosPorCnae(String token, String cnae, String tipo, String cursor, int size) {
		return autenticado(token).get()
				.uri(b -> {
					var uri = b.path("/api/estabelecimentos")
							.queryParam("cnae", cnae)
							.queryParam("tipoCnae", tipo)
							.queryParam("size", size);
					if (cursor != null) {
						uri = uri.queryParam("cursor", cursor);
					}
					return uri.build();
				})
				.retrieve()
				.body(JsonNode.class);
	}

	@Test
	void listarEmpresasSemFiltroRetornaPagina() {
		JsonNode corpo = autenticado(tokenAdmin()).get()
				.uri("/api/empresas")
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("content").isArray()).isTrue();
		assertThat(corpo.get("totalElements").isNumber()).isTrue();
	}

	@Test
	void buscarEmpresaPorCnpjBasico() {
		JsonNode corpo = empresas(tokenAdmin(), "cnpjBasico", "08453900");

		assertThat(corpo.get("content").size()).isGreaterThan(0);
		assertThat(corpo.at("/content/0/cnpjBasico").asText()).isEqualTo("08453900");
	}

	@Test
	void detalharEmpresaInexistenteRetorna404() {
		assertThatThrownBy(() -> autenticado(tokenAdmin()).get()
				.uri("/api/empresas/99999999")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.NotFound.class);
	}

	@Test
	void filtrarEstabelecimentosPorUf() {
		JsonNode corpo = estabelecimentos(tokenUsuario(), "uf", "SP");

		assertThat(corpo.get("content").size()).isGreaterThan(0);
		assertThat(corpo.at("/content/0/uf").asText()).isEqualTo("SP");
	}

	@Test
	void filtrarEstabelecimentosPorCnae() {
		JsonNode corpo = estabelecimentos(tokenUsuario(), "cnae", "4712100");

		assertThat(corpo.get("totalElements").asLong()).isGreaterThan(0);
		assertThat(corpo.at("/content/0/cnaeFiscalPrincipal").asText()).isEqualTo("4712100");
	}

	@Test
	void filtrarPorCnaePrincipalIgnoraCnaeSecundario() {
		JsonNode principal = estabelecimentosPorCnae(tokenUsuario(), "4712100", "principal", null, 20);
		JsonNode secundarios = estabelecimentosPorCnae(tokenUsuario(), "4712100", "secundario", null, 20);

		// Os dois conjuntos podem intersectar, mas o filtro por principal não pode
		// devolver linhas cujo CNAE principal é outro.
		for (JsonNode linha : principal.get("content")) {
			assertThat(linha.get("cnaeFiscalPrincipal").asText()).isEqualTo("4712100");
		}
		for (JsonNode linha : secundarios.get("content")) {
			assertThat(linha.get("cnaeFiscalSecundaria").asText()).contains("4712100");
		}
	}

	@Test
	void filtrarPorCnaeAmbosAceitaPrincipalESecundario() {
		JsonNode corpo = estabelecimentosPorCnae(tokenUsuario(), "4712100", "ambos", null, 50);

		assertThat(corpo.get("content").size()).isGreaterThan(0);
		for (JsonNode linha : corpo.get("content")) {
			boolean casa = "4712100".equals(linha.get("cnaeFiscalPrincipal").asText())
					|| linha.get("cnaeFiscalSecundaria").asText().contains("4712100");
			assertThat(casa).isTrue();
		}
	}

	@Test
	void paginacaoPorCursorNaoRepeteNemPulaLinhas() {
		String token = tokenUsuario();
		JsonNode primeira = estabelecimentosPorCnae(token, "4712100", "principal", null, 20);

		assertThat(primeira.get("content").size()).isEqualTo(20);
		String cursor = primeira.get("nextCursor").asText();
		assertThat(cursor).isNotBlank();

		JsonNode segunda = estabelecimentosPorCnae(token, "4712100", "principal", cursor, 20);

		assertThat(segunda.get("content").size()).isEqualTo(20);
		// O cursor retoma exatamente após a última linha da página anterior.
		String ultimoPrimeiro = primeira.at("/content/19/cnpjBasico").asText() + primeira.at("/content/19/cnpjOrdem").asText();
		String chaveSegunda = segunda.at("/content/0/cnpjBasico").asText() + segunda.at("/content/0/cnpjOrdem").asText();
		assertThat(chaveSegunda).isGreaterThan(ultimoPrimeiro);

		// Percorrer o cursor tem de reproduzir a paginação por offset, sem lacunas
		// nem repetições.
		JsonNode porOffset = autenticado(token).get()
				.uri(b -> b.path("/api/estabelecimentos")
						.queryParam("cnae", "4712100")
						.queryParam("tipoCnae", "principal")
						.queryParam("size", 20)
						.queryParam("page", 1)
						.build())
				.retrieve()
				.body(JsonNode.class);
		for (int i = 0; i < 20; i++) {
			assertThat(segunda.at("/content/" + i + "/cnpjCompleto").asText())
					.isEqualTo(porOffset.at("/content/" + i + "/cnpjCompleto").asText());
		}
	}

	@Test
	void paginaFinalNaoDevolveCursor() {
		String token = tokenUsuario();
		JsonNode unica = estabelecimentosPorCnae(token, "4712100", "principal", null, 100);

		// A última página não pode anunciar próxima página inexistente.
		assertThat(unica.get("content").size()).isLessThanOrEqualTo(100);
		if (unica.get("content").size() < 100) {
			assertThat(unica.get("nextCursor").isNull()).isTrue();
		}
	}

	@Test
	void cursorMalformadoCaiParaPrimeiraPagina() {
		JsonNode comCursor = autenticado(tokenUsuario()).get()
				.uri(b -> b.path("/api/estabelecimentos")
						.queryParam("cnae", "4712100")
						.queryParam("tipoCnae", "principal")
						.queryParam("cursor", "nao-e-um-cursor-valido!!")
						.build())
				.retrieve()
				.body(JsonNode.class);
		JsonNode semCursor = estabelecimentosPorCnae(tokenUsuario(), "4712100", "principal", null, 20);

		assertThat(comCursor.at("/content/0/cnpjCompleto").asText())
				.isEqualTo(semCursor.at("/content/0/cnpjCompleto").asText());
	}

	@Test
	void detalharEstabelecimento() {
		JsonNode corpo = autenticado(tokenUsuario()).get()
				.uri("/api/estabelecimentos/43136185/0001/79")
				.retrieve()
				.body(JsonNode.class);

		assertThat(corpo.get("cnpjCompleto").asText()).isEqualTo("43136185000179");
		assertThat(corpo.get("razaoSocial").asText()).isNotBlank();
		assertThat(corpo.at("/cnaeFiscalPrincipal").asText()).isEqualTo("4723700");
		assertThat(corpo.at("/cnaeDescricao").asText()).isNotBlank();
		assertThat(corpo.at("/naturezaJuridica").asText()).isEqualTo("2135");
		assertThat(corpo.at("/naturezaJuridicaDescricao").asText()).isEqualTo("Empresário (Individual)");
		assertThat(corpo.at("/municipio").asText()).isEqualTo("6493");
		assertThat(corpo.at("/municipioDescricao").asText()).isEqualTo("IBITINGA");
	}

	@Test
	void detalharEstabelecimentoInexistenteRetorna404() {
		assertThatThrownBy(() -> autenticado(tokenAdmin()).get()
				.uri("/api/estabelecimentos/43136185/9999/99")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.NotFound.class);
	}

	@Test
	void dominiosCarregam() {
		String token = tokenUsuario();

		JsonNode cnaes = autenticado(token).get().uri("/api/dominios/cnaes").retrieve().body(JsonNode.class);
		JsonNode naturezas = autenticado(token).get().uri("/api/dominios/naturezas").retrieve().body(JsonNode.class);

		assertThat(cnaes.isArray()).isTrue();
		assertThat(cnaes.size()).isGreaterThan(0);
		assertThat(cnaes.get(0).get("codigo").asText()).isNotBlank();
		assertThat(naturezas.isArray()).isTrue();
		assertThat(naturezas.get(0).get("codigo").asText()).isNotBlank();
	}

	@Test
	void exportarEmpresas() {
		byte[] corpo = autenticado(tokenUsuario()).get()
				.uri("/api/exportacao/empresas?limite=5")
				.retrieve()
				.body(byte[].class);

		assertThat(corpo).isNotEmpty();
		assertThat(corpo[0]).isEqualTo((byte) 'P');
		assertThat(corpo[1]).isEqualTo((byte) 'K');
	}

	@Test
	void exportarEstabelecimentos() {
		byte[] corpo = autenticado(tokenUsuario()).get()
				.uri("/api/exportacao/estabelecimentos?cnpjBasico=43136185&limite=10")
				.retrieve()
				.body(byte[].class);

		assertThat(corpo).isNotEmpty();
		assertThat(corpo[0]).isEqualTo((byte) 'P');
		assertThat(corpo[1]).isEqualTo((byte) 'K');
	}

	@Test
	void exportarEstabelecimentosComCnaeEFiltros() {
		byte[] corpo = autenticado(tokenUsuario()).get()
				.uri("/api/exportacao/estabelecimentos?cnpjBasico=43136185&cnae=6201500&tipoCnae=principal&uf=SP&situacao=02&limite=10")
				.retrieve()
				.body(byte[].class);

		assertThat(corpo).isNotEmpty();
		assertThat(corpo[0]).isEqualTo((byte) 'P');
		assertThat(corpo[1]).isEqualTo((byte) 'K');
	}

	@Test
	void exportarCnaes() {
		byte[] corpo = autenticado(tokenUsuario()).get()
				.uri("/api/exportacao/cnaes")
				.retrieve()
				.body(byte[].class);

		assertThat(corpo).isNotEmpty();
		assertThat(corpo[0]).isEqualTo((byte) 'P');
		assertThat(corpo[1]).isEqualTo((byte) 'K');
	}

	@Test
	void exportacaoExigeAutenticacao() {
		assertThatThrownBy(() -> rest().get()
				.uri("/api/exportacao/empresas?limite=5")
				.retrieve()
				.toBodilessEntity())
				.isInstanceOf(HttpClientErrorException.Unauthorized.class);
	}
}