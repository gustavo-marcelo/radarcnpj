import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { ConfigImportacao, EstadoImportacao, StatusImportacao } from '../../core/models/importacao.model';
import { ApiService } from '../../core/services/api.service';

const POLL_MS = 2000;

@Component({
  selector: 'app-importacao',
  imports: [FormsModule],
  templateUrl: './importacao.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportacaoAdmin implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);

  dadosDir = '';
  threads = 4;
  truncateAntes = true;

  config = signal<ConfigImportacao | null>(null);
  status = signal<StatusImportacao | null>(null);
  carregando = signal(false);
  erro = signal('');
  aviso = signal('');

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  estadoLabel(estado: EstadoImportacao): string {
    return {
      IDLE: 'Ocioso',
      RUNNING: 'Executando',
      DONE: 'Concluído',
      ERROR: 'Erro',
      CANCELED: 'Cancelado',
    }[estado] ?? estado;
  }

  emExecucao(): boolean {
    return this.status()?.estado === 'RUNNING';
  }

  ngOnInit(): void {
    void this.carregarConfig();
    void this.atualizarStatus();
    this.iniciarPolling();
  }

  ngOnDestroy(): void {
    this.pararPolling();
  }

  async carregarConfig(): Promise<void> {
    try {
      const config = await firstValueFrom(this.api.get<ConfigImportacao>('/config/importacao'));
      this.config.set(config);
      this.dadosDir = config.dadosDir;
      this.threads = config.threads;
      this.truncateAntes = config.truncateAntes;
    } catch (e) {
      this.erroErro(e, 'Erro ao carregar configuração.');
    }
  }

  async salvarConfig(): Promise<void> {
    try {
      const salva = await firstValueFrom(
        this.api.put<ConfigImportacao>('/config/importacao', {
          dadosDir: this.dadosDir,
          threads: this.threads,
          truncateAntes: this.truncateAntes,
        }),
      );
      this.config.set(salva);
      this.aviso.set('Configuração salva.');
    } catch (e) {
      this.erroErro(e, 'Erro ao salvar configuração.');
    }
  }

  async iniciar(): Promise<void> {
    this.erro.set('');
    this.aviso.set('');
    try {
      await firstValueFrom(this.api.post('/importacao/iniciar'));
      await this.atualizarStatus();
    } catch (e) {
      this.erroErro(e, 'Erro ao iniciar importação.');
    }
  }

  async cancelar(): Promise<void> {
    try {
      await firstValueFrom(this.api.post('/importacao/cancelar'));
      await this.atualizarStatus();
    } catch (e) {
      this.erroErro(e, 'Erro ao cancelar importação.');
    }
  }

  async atualizarStatus(): Promise<void> {
    try {
      const status = await firstValueFrom(this.api.get<StatusImportacao>('/importacao/status'));
      this.status.set(status);
    } catch (e) {
      this.erroErro(e, 'Erro ao consultar status.');
    }
  }

  private iniciarPolling(): void {
    this.pollTimer = setInterval(() => {
      if (this.status()?.estado === 'RUNNING') {
        void this.atualizarStatus();
      }
    }, POLL_MS);
  }

  private pararPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  private erroErro(e: unknown, padrao: string): void {
    this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? padrao);
  }

  limparAviso(): void {
    this.aviso.set('');
  }
}