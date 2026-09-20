import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { TypeEstabelecimentoDetalhe, PORTE_LABEL } from '../../core/models/estabelecimento-detalhe.model';
import { MATRIZ_FILIAL_LABEL, SITUACAO_LABEL } from '../../core/models/estabelecimento.model';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-estabelecimento-detalhe',
  imports: [RouterLink],
  templateUrl: './estabelecimento-detalhe.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstabelecimentoDetalhe implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);

  detalhe = signal<TypeEstabelecimentoDetalhe | null>(null);
  carregando = signal(true);
  erro = signal('');

  origemLabel(o: string): string {
    return MATRIZ_FILIAL_LABEL[o] ?? o;
  }

  situacaoLabel(s: string): string {
    return SITUACAO_LABEL[s] ?? s;
  }

  porteLabel(porte: string): string {
    return PORTE_LABEL[porte] ?? porte;
  }

  ngOnInit(): void {
    const basico = this.route.snapshot.paramMap.get('cnpjBasico') ?? '';
    const ordem = this.route.snapshot.paramMap.get('cnpjOrdem') ?? '';
    const dv = this.route.snapshot.paramMap.get('cnpjDv') ?? '';
    void this.carregar(basico, ordem, dv);
  }

  async carregar(basico: string, ordem: string, dv: string): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    try {
      const detalhe = await firstValueFrom(
        this.api.get<TypeEstabelecimentoDetalhe>(`/estabelecimentos/${basico}/${ordem}/${dv}`),
      );
      this.detalhe.set(detalhe);
    } catch (e) {
      this.erro.set(
        (e as { error?: { detail?: string } })?.error?.detail ?? 'Estabelecimento não encontrado.',
      );
    } finally {
      this.carregando.set(false);
    }
  }

  fone(ddd: string | null, numero: string | null): string | null {
    return ddd && numero ? `(${ddd}) ${numero}` : null;
  }

  endereco(d: TypeEstabelecimentoDetalhe): string {
    const partes = [
      d.tipoLogradouro ? `${d.tipoLogradouro} ${d.logradouro}` : d.logradouro,
      d.numero,
      d.complemento,
      d.bairro,
    ].filter(Boolean);
    return partes.join(', ');
  }

  cnaesSecundarios(d: TypeEstabelecimentoDetalhe): string[] {
    return d.cnaeFiscalSecundaria ? d.cnaeFiscalSecundaria.split(',').map((c) => c.trim()).filter(Boolean) : [];
  }
}