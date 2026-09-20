import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { PORTE_LABEL, TypeEmpresaDetalhe } from '../../core/models/empresa.model';
import { MATRIZ_FILIAL_LABEL, SITUACAO_LABEL } from '../../core/models/estabelecimento.model';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-empresa-detalhe',
  imports: [RouterLink],
  templateUrl: './empresa-detalhe.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmpresaDetalhe implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  detalhe = signal<TypeEmpresaDetalhe | null>(null);
  carregando = signal(true);
  erro = signal('');

  porteLabel(porte: string): string {
    return PORTE_LABEL[porte] ?? porte;
  }

  situacaoLabel(s: string): string {
    return SITUACAO_LABEL[s] ?? s;
  }

  origemLabel(o: string): string {
    return MATRIZ_FILIAL_LABEL[o] ?? o;
  }

  ngOnInit(): void {
    const basico = this.route.snapshot.paramMap.get('cnpjBasico') ?? '';
    void this.carregar(basico);
  }

  async carregar(basico: string): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    try {
      const detalhe = await firstValueFrom(this.api.get<TypeEmpresaDetalhe>(`/empresas/${basico}`));
      this.detalhe.set(detalhe);
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Empresa não encontrada.');
    } finally {
      this.carregando.set(false);
    }
  }
}