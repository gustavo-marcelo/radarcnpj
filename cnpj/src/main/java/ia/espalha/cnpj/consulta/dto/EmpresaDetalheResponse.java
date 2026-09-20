package ia.espalha.cnpj.consulta.dto;

import java.util.List;

public record EmpresaDetalheResponse(EmpresaResponse empresa, List<EstabelecimentoResponse> estabelecimentos) {
}