import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-cadastro-usuario',
  imports: [FormsModule, RouterLink],
  templateUrl: './cadastro-usuario.html',
  styleUrl: './cadastro-usuario.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CadastroUsuario {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  nome = '';
  email = '';
  senha = '';
  carregando = signal(false);
  erro = signal('');
  sucesso = signal(false);

  async cadastrar(): Promise<void> {
    if (!this.nome || !this.email || this.senha.length < 6 || this.carregando()) {
      return;
    }
    this.carregando.set(true);
    this.erro.set('');
    try {
      await this.auth.registrar({ nome: this.nome, email: this.email, senha: this.senha });
      this.sucesso.set(true);
    } catch (e) {
      const status = (e as { status?: number })?.status;
      if (status === 403) {
        this.erro.set('Cadastro aberto não está habilitado neste ambiente. Procure um administrador.');
      } else {
        this.erro.set(this.detail(e) ?? 'Não foi possível concluir o cadastro.');
      }
    } finally {
      this.carregando.set(false);
    }
  }

  private detail(e: unknown): string | undefined {
    return (e as { error?: { detail?: string } })?.error?.detail;
  }
}