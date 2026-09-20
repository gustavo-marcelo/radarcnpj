import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;
  let requisicao = req;
  if (token) {
    requisicao = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(requisicao).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status === 401 && token && router.url !== '/login') {
        auth.deslogar();
        void router.navigate(['/login']);
      }
      return throwError(() => erro);
    }),
  );
};