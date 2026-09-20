import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly baseUrl = environment.apiBaseUrl;
  private readonly http = inject(HttpClient);

  get<T>(caminho: string, params?: HttpParams) {
    return this.http.get<T>(`${this.baseUrl}${caminho}`, { params });
  }

  blob(caminho: string, params?: HttpParams) {
    return this.http.get(`${this.baseUrl}${caminho}`, { params, responseType: 'blob' });
  }

  post<T>(caminho: string, corpo?: unknown) {
    return this.http.post<T>(`${this.baseUrl}${caminho}`, corpo);
  }

  put<T>(caminho: string, corpo?: unknown) {
    return this.http.put<T>(`${this.baseUrl}${caminho}`, corpo);
  }

  delete<T>(caminho: string) {
    return this.http.delete<T>(`${this.baseUrl}${caminho}`);
  }
}