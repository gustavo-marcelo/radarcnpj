import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Layout {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly menuAberto = signal(false);

  usuarioAtual() {
    return this.auth.usuario();
  }

  sair(): void {
    this.auth.deslogar();
    void this.router.navigate(['/login']);
  }
}