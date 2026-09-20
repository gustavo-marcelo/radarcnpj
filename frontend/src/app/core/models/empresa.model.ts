import { Estabelecimento } from './estabelecimento.model';

export interface Empresa {
  cnpjBasico: string;
  razaoSocial: string;
  naturezaJuridica: string;
  qualificacaoResponsavel: string;
  capitalSocial: number;
  porte: string;
  enteFederativo: string | null;
}

export interface TypeEmpresaDetalhe {
  empresa: Empresa;
  estabelecimentos: Estabelecimento[];
}

export const PORTE_LABEL: Record<string, string> = {
  '00': 'Não informado',
  '01': 'Micro Empresa',
  '03': 'Empresa de Pequeno Porte',
  '05': 'Demais',
};