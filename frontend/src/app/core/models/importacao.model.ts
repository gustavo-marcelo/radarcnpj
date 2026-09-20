export type EstadoImportacao = 'IDLE' | 'RUNNING' | 'DONE' | 'ERROR' | 'CANCELED';

export interface StatusImportacao {
  estado: EstadoImportacao;
  tabelaAtual: string | null;
  arquivoAtual: string | null;
  linhasImportadas: number;
  iniciadoEm: string | null;
  finalizadoEm: string | null;
  mensagem: string | null;
}

export interface ConfigImportacao {
  dadosDir: string;
  threads: number;
  truncateAntes: boolean;
}