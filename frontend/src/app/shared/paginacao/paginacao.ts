import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-paginacao',
  imports: [],
  templateUrl: './paginacao.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Paginacao {
  readonly total = input<number>(0);
  readonly totalPaginas = input<number>(0);
  readonly paginaAtual = input<number>(0);

  readonly pagina = output<number>();

  temAnterior(): boolean {
    return this.paginaAtual() > 0;
  }

  temProxima(): boolean {
    return this.paginaAtual() + 1 < this.totalPaginas();
  }

  ir(p: number): void {
    if (p >= 0 && p < this.totalPaginas() && p !== this.paginaAtual()) {
      this.pagina.emit(p);
    }
  }
}