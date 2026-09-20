import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '../services/api.service';
import { LoginRequest, LoginResponse, RegistrarRequest, Usuario } from '../models/usuario.model';

const TOKEN_KEY = 'radar-cnpj.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  readonly usuario = signal<Usuario | null>(null);
  readonly inicializado = signal(false);

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAutenticado(): boolean {
    return (this.usuario() ?? null) !== null || !!this.token;
  }

  isAdmin(): boolean {
    return this.usuario()?.papel === 'ADMIN';
  }

  async login(credenciais: LoginRequest): Promise<Usuario> {
    const resposta = await firstValueFrom(
      this.api.post<LoginResponse>('/auth/login', credenciais),
    );
    localStorage.setItem(TOKEN_KEY, resposta.token);
    this.usuario.set(resposta.usuario);
    return resposta.usuario;
  }

  async registrar(dados: RegistrarRequest): Promise<void> {
    await firstValueFrom(this.api.post<unknown>('/auth/registrar', dados));
  }

  async restaurarSessao(): Promise<void> {
    const token = this.token;
    if (!token) {
      this.inicializado.set(true);
      return;
    }
    try {
      const usuario = await firstValueFrom(this.http.get<Usuario>(`${this.api.baseUrl}/auth/me`));
      this.usuario.set(usuario);
    } catch {
      this.deslogar();
    } finally {
      this.inicializado.set(true);
    }
  }

  deslogar(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.usuario.set(null);
  }
}