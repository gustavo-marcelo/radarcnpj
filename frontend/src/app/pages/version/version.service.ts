import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import packageInfo from '../../../../package.json';

@Injectable({
  providedIn: 'root',
})
export class VersionService {
  private http = inject(HttpClient);

  // Versão obtida dinamicamente do package.json
  private readonly version = packageInfo.version;

  getVersion(): string {
    return this.version;
  }
}
