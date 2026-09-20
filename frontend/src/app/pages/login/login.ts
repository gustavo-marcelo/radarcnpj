import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  email = '';
  senha = '';
  carregando = signal(false);
  erro = signal('');

  async entrar(): Promise<void> {
    if (!this.email || !this.senha || this.carregando()) {
      return;
    }
    this.carregando.set(true);
    this.erro.set('');
    try {
      await this.auth.login({ email: this.email, senha: this.senha });
      void this.router.navigate(['/empresas']);
    } catch (e) {
      this.erro.set(this.mensagemErro(e, 'E-mail ou senha inválidos.'));
    } finally {
      this.carregando.set(false);
    }
  }

  private mensagemErro(e: unknown, padrao: string): string {
    const detail = (e as { error?: { detail?: string } })?.error?.detail;
    return detail || padrao;
  }
}