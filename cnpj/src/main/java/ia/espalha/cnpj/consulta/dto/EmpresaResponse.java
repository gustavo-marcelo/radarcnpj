package ia.espalha.cnpj.consulta.dto;

import java.math.BigDecimal;

public record EmpresaResponse(
		String cnpjBasico,
		String razaoSocial,
		String naturezaJuridica,
		String qualificacaoResponsavel,
		BigDecimal capitalSocial,
		String porte,
		String enteFederativo) {
}