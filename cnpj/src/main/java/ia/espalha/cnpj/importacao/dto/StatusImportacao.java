package ia.espalha.cnpj.importacao.dto;

import java.time.Instant;

import ia.espalha.cnpj.importacao.EstadoImportacao;

public record StatusImportacao(
		EstadoImportacao estado,
		String tabelaAtual,
		String arquivoAtual,
		long linhasImportadas,
		Instant iniciadoEm,
		Instant finalizadoEm,
		String mensagem) {
}