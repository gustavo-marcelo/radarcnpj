package ia.espalha.cnpj.consulta.dto;

import java.time.LocalDate;

public record EstabelecimentoResponse(
		String cnpjBasico,
		String cnpjOrdem,
		String cnpjDv,
		String cnpjCompleto,
		String identificadorMatrizFilial,
		String nomeFantasia,
		String situacaoCadastral,
		LocalDate dataSituacaoCadastral,
		String motivoSituacaoCadastral,
		String cnaeFiscalPrincipal,
		String cnaeFiscalSecundaria,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cep,
		String uf,
		String municipio,
		String municipioDescricao,
		String ddd1,
		String telefone1,
		String correioEletronico,
		String razaoSocial,
		String porte) {
}