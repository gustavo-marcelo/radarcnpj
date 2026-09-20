export interface TypeEstabelecimentoDetalhe {
  cnpjBasico: string;
  cnpjOrdem: string;
  cnpjDv: string;
  cnpjCompleto: string;
  identificadorMatrizFilial: string;
  nomeFantasia: string | null;
  situacaoCadastral: string;
  dataSituacaoCadastral: string | null;
  motivoSituacaoCadastral: string;
  dataInicioAtividade: string | null;
  pais: string | null;
  cnaeFiscalPrincipal: string;
  cnaeFiscalSecundaria: string | null;
  cnaeDescricao: string | null;
  tipoLogradouro: string | null;
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cep: string;
  uf: string;
  municipio: string;
  municipioDescricao: string | null;
  nomeCidadeExterior: string | null;
  ddd1: string | null;
  telefone1: string | null;
  ddd2: string | null;
  telefone2: string | null;
  dddFax: string | null;
  fax: string | null;
  correioEletronico: string | null;
  situacaoEspecial: string | null;
  dataSituacaoEspecial: string | null;
  razaoSocial: string | null;
  porte: string | null;
  naturezaJuridica: string;
  naturezaJuridicaDescricao: string | null;
  qualificacaoResponsavel: string;
  capitalSocial: number;
  enteFederativo: string | null;
}

export const PORTE_LABEL: Record<string, string> = {
  '00': 'Não informado',
  '01': 'Micro Empresa',
  '03': 'Empresa de Pequeno Porte',
  '05': 'Demais',
};