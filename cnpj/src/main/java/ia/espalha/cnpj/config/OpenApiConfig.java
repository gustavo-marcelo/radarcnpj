package ia.espalha.cnpj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openApi() {
		var bearer = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");
		return new OpenAPI()
				.info(new Info()
						.title("API CNPJ")
						.description("Autenticação, importação e consulta de dados de CNPJ.")
						.version("v1"))
				.components(new Components().addSecuritySchemes("bearer", bearer))
				.addSecurityItem(new SecurityRequirement().addList("bearer"));
	}
}