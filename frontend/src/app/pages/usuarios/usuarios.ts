import { HttpParams } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { Paginacao } from '../../shared/paginacao/paginacao';
import { Pagina } from '../../core/models/pagina.model';
import { CriarUsuarioRequest, Usuario } from '../../core/models/usuario.model';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-usuarios',
  imports: [FormsModule, Paginacao],
  templateUrl: './usuarios.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Usuarios {
  private readonly api = inject(ApiService);

  q = '';
  mostrarForm = signal(false);
  nome = '';
  email = '';
  senha = '';
  papel: 'ADMIN' | 'USER' = 'USER';

  carregando = signal(false);
  erro = signal('');
  aviso = signal('');
  pagina = signal<Pagina<Usuario> | null>(null);

  async buscar(p = 0): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    const params = new HttpParams().set('page', p).set('size', 20).set('q', this.q.trim());
    try {
      const resposta = await firstValueFrom(this.api.get<Pagina<Usuario>>('/usuarios', params));
      this.pagina.set(resposta);
    } catch (e) {
      this.pagina.set(null);
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Erro ao listar usuários.');
    } finally {
      this.carregando.set(false);
    }
  }

  async criar(): Promise<void> {
    if (!this.nome || !this.email || this.senha.length < 6) {
      return;
    }
    this.erro.set('');
    const corpo: CriarUsuarioRequest = {
      nome: this.nome,
      email: this.email,
      senha: this.senha,
      papel: this.papel,
    };
    try {
      await firstValueFrom(this.api.post<Usuario>('/usuarios', corpo));
      this.mostrarForm.set(false);
      this.nome = '';
      this.email = '';
      this.senha = '';
      this.papel = 'USER';
      this.aviso.set('Usuário criado com sucesso.');
      await this.buscar(0);
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Não foi possível criar o usuário.');
    }
  }

  async alternarAtivo(u: Usuario): Promise<void> {
    try {
      await firstValueFrom(
        this.api.put<Usuario>(`/usuarios/${u.id}`, { nome: u.nome, papel: u.papel, ativo: !u.ativo }),
      );
      await this.buscar(0);
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Não foi possível atualizar o usuário.');
    }
  }

  async remover(u: Usuario): Promise<void> {
    if (!confirm(`Remover o usuário "${u.nome}"?`)) {
      return;
    }
    try {
      await firstValueFrom(this.api.delete(`/usuarios/${u.id}`));
      this.aviso.set('Usuário removido.');
      await this.buscar(0);
    } catch (e) {
      this.erro.set((e as { error?: { detail?: string } })?.error?.detail ?? 'Não foi possível remover o usuário.');
    }
  }

  novaPagina(p: number): void {
    void this.buscar(p);
  }

  limparAviso(): void {
    this.aviso.set('');
  }
}