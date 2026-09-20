import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import { VersionService } from './version.service';

@Component({
  selector: 'app-version',
  imports: [],
  templateUrl: './version.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./version.css'],
})
export class Version implements OnInit {
  appVersion!: string;
  apiVersion = 'Carregando...';
  versionService = inject(VersionService);

  ngOnInit() {
    this.appVersion = this.versionService.getVersion();
    scrollTo(0, 0);
  }

  getCurrentDate(): string {
    const now = new Date();
    return now.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }
}
