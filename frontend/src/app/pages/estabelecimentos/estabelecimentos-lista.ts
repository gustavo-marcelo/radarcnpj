import { HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Paginacao } from '../../shared/paginacao/paginacao';
import { baixarArquivo } from '../../shared/download';
import { Dominio } from '../../core/models/dominio.model';
import { Estabelecimento, MATRIZ_FILIAL_LABEL, SITUACAO_LABEL } from '../../core/models/estabelecimento.model';
import { Pagina } from '../../core/models/pagina.model';
import { ApiService } from '../../core/services/api.service';

const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
  'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
];

@Component({
  selector: 'app-estabelecimentos-lista',
  imports: [FormsModule, RouterLink, Paginacao],
  templateUrl: './estabelecimentos-lista.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstabelecimentosLista implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  razaoSocial = '';
  nomeFantasia = '';
  cnpjBasico = '';
  uf = '';
  municipio = '';
  cnae = '';
  tipoCnae = 'principal';
  situacao = '';
  limite = 5000;

  ufs = signal(UFS);
  cnaes = signal<Dominio[]>([]);
  situacoes = signal<Dominio[]>(
    Object.entries(SITUACAO_LABEL).map(([codigo, descricao]) => ({ codigo, descricao })),
  );

  carregando = signal(false);
  erro = signal('');
  pagina = signal<Pagina<Estabelecimento> | null>(null);

  situacaoLabel(s: string): string {
    return SITUACAO_LABEL[s] ?? s;
  }

  origemLabel(o: string): string {
    return MATRIZ_FILIAL_LABEL[o] ?? o;
  }

  constructor() {
    this.api.get<Dominio[]>('/dominios/cnaes').subscribe({
      next: (v) => this.cnaes.set(v),
      error: () => void 0,
    });
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const cnpjBasico = params.get('cnpjBasico');
      const cnae = params.get('cnae');
      const situacao = params.get('situacao');
      if (cnpjBasico) {
        this.cnpjBasico = cnpjBasico;
      }
      if (cnae) {
        this.cnae = cnae;
      }
      if (situacao) {
        this.situacao = situacao;
      }
      if (cnpjBasico || cnae || situacao) {
        void this.buscar(0);
      }
    });
  }

  async buscar(pagina = 0): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    const params = new HttpParams()
      .set('page', pagina)
      .set('size', 20)
      .set('razaoSocial', this.razaoSocial.trim())
      .set('nomeFantasia', this.nomeFantasia.trim())
      .set('cnpjBasico', this.cnpjBasico.trim())
      .set('uf', this.uf)
      .set('municipio', this.municipio.trim())
      .set('cnae', this.cnae.trim())
      .set('tipoCnae', this.tipoCnae)
      .set('situacao', this.situacao);
    try {
      const resposta = await firstValueFrom(this.api.get<Pagina<Estabelecimento>>('/estabelecimentos', params));
      this.pagina.set(resposta);
    } catch (e) {
      this.pagina.set(null);
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao listar estabelecimentos.');
    } finally {
      this.carregando.set(false);
    }
  }

  novaPagina(p: number): void {
    void this.buscar(p);
  }

  async exportar(): Promise<void> {
    if (this.carregando()) {
      return;
    }
    this.carregando.set(true);
    this.erro.set('');
    const params = new HttpParams()
      .set('limite', this.limite)
      .set('razaoSocial', this.razaoSocial.trim())
      .set('nomeFantasia', this.nomeFantasia.trim())
      .set('cnpjBasico', this.cnpjBasico.trim())
      .set('uf', this.uf)
      .set('municipio', this.municipio.trim())
      .set('cnae', this.cnae.trim())
      .set('tipoCnae', this.tipoCnae)
      .set('situacao', this.situacao);
    try {
      const blob = await firstValueFrom(this.api.blob('/exportacao/estabelecimentos', params));
      baixarArquivo(blob, 'estabelecimentos.xlsx');
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao exportar estabelecimentos.');
    } finally {
      this.carregando.set(false);
    }
  }
}