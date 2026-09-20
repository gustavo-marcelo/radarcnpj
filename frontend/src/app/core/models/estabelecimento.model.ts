export interface Estabelecimento {
  cnpjBasico: string;
  cnpjOrdem: string;
  cnpjDv: string;
  cnpjCompleto: string;
  identificadorMatrizFilial: string;
  nomeFantasia: string | null;
  situacaoCadastral: string;
  dataSituacaoCadastral: string | null;
  motivoSituacaoCadastral: string;
  cnaeFiscalPrincipal: string;
  cnaeFiscalSecundaria: string | null;
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cep: string;
  uf: string;
  municipio: string;
  municipioDescricao: string | null;
  ddd1: string | null;
  telefone1: string | null;
  correioEletronico: string | null;
  razaoSocial: string | null;
  porte: string | null;
}

export const SITUACAO_LABEL: Record<string, string> = {
  '01': 'Nula',
  '02': 'Ativa',
  '03': 'Suspensa',
  '04': 'Inapta',
  '08': 'Baixada',
};

export const MATRIZ_FILIAL_LABEL: Record<string, string> = {
  '1': 'Matriz',
  '2': 'Filial',
};