import { HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Paginacao } from '../../shared/paginacao/paginacao';
import { baixarArquivo } from '../../shared/download';
import { Dominio } from '../../core/models/dominio.model';
import { Empresa } from '../../core/models/empresa.model';
import { Pagina } from '../../core/models/pagina.model';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-empresas-lista',
  imports: [FormsModule, RouterLink, Paginacao],
  templateUrl: './empresas-lista.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmpresasLista {
  private readonly api = inject(ApiService);

  razaoSocial = '';
  cnpjBasico = '';
  natureza = '';
  limite = 5000;
  naturezas = signal<Dominio[]>([]);

  carregando = signal(false);
  erro = signal('');
  pagina = signal<Pagina<Empresa> | null>(null);

  constructor() {
    this.api.get<Dominio[]>('/dominios/naturezas').subscribe({
      next: (v) => this.naturezas.set(v),
      error: () => void 0,
    });
  }

  async buscar(pagina = 0): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    const params = new HttpParams()
      .set('page', pagina)
      .set('size', 20)
      .set('razaoSocial', this.razaoSocial.trim())
      .set('cnpjBasico', this.cnpjBasico.trim())
      .set('natureza', this.natureza);
    try {
      const resposta = await firstValueFrom(this.api.get<Pagina<Empresa>>('/empresas', params));
      this.pagina.set(resposta);
    } catch (e) {
      this.pagina.set(null);
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao listar empresas.');
    } finally {
      this.carregando.set(false);
    }
  }

  novaPagina(p: number): void {
    void this.buscar(p);
  }

  linkEmpresa(basico: string): string {
    return `/empresas/${basico}`;
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
      .set('cnpjBasico', this.cnpjBasico.trim())
      .set('natureza', this.natureza);
    try {
      const blob = await firstValueFrom(this.api.blob('/exportacao/empresas', params));
      baixarArquivo(blob, 'empresas.xlsx');
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao exportar empresas.');
    } finally {
      this.carregando.set(false);
    }
  }
}