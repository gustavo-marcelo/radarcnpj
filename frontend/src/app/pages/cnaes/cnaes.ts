import { HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Dominio } from '../../core/models/dominio.model';
import { ApiService } from '../../core/services/api.service';
import { baixarArquivo } from '../../shared/download';

@Component({
  selector: 'app-cnaes',
  imports: [FormsModule, RouterLink],
  templateUrl: './cnaes.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cnaes {
  private readonly api = inject(ApiService);

  q = '';
  resultados = signal<Dominio[]>([]);
  total = signal(0);
  carregando = signal(false);
  erro = signal('');
  buscou = signal(false);

  async buscar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    const params = new HttpParams().set('q', this.q.trim());
    try {
      const resposta = await firstValueFrom(this.api.get<Dominio[]>('/dominios/cnaes', params));
      this.resultados.set(resposta);
      this.total.set(resposta.length);
      this.buscou.set(true);
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao consultar CNAEs.');
    } finally {
      this.carregando.set(false);
    }
  }

  async exportar(): Promise<void> {
    if (this.carregando()) {
      return;
    }
    this.carregando.set(true);
    this.erro.set('');
    const params = new HttpParams().set('q', this.q.trim());
    try {
      const blob = await firstValueFrom(this.api.blob('/exportacao/cnaes', params));
      baixarArquivo(blob, 'cnaes.xlsx');
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao exportar CNAEs.');
    } finally {
      this.carregando.set(false);
    }
  }
}