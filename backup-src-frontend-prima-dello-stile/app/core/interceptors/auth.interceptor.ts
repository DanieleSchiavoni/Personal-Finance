// src/app/core/interceptors/auth.interceptor.ts
import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AutenticazioneService } from '../services/autenticazione.service';

/**
 * Aggiunge il token a ogni chiamata verso le nostre API. Se il token è scaduto (401) ne chiede uno nuovo
 * con il refresh token e ripete la chiamata una sola volta.
 */
export const authInterceptor: HttpInterceptorFn = (richiesta, avanti) => {
  const auth = inject(AutenticazioneService);

  // Il token si invia solo al nostro backend; le chiamate di autenticazione non lo richiedono
  const nostroBackend = richiesta.url.startsWith(environment.apiUrl);
  const chiamataDiAutenticazione = richiesta.url.startsWith(`${environment.apiUrl}/auth/`);
  if (!nostroBackend || chiamataDiAutenticazione) {
    return avanti(richiesta);
  }

  const conToken = (originale: HttpRequest<unknown>) => {
    const token = auth.getAccessToken();
    return token ? originale.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : originale;
  };

  return avanti(conToken(richiesta)).pipe(
    catchError((errore: unknown) => {
      if (errore instanceof HttpErrorResponse && errore.status === 401 && auth.haRefreshToken()) {
        return auth.rinnova().pipe(
          // Se il rinnovo fallisce la sessione è finita: si torna al login
          catchError((erroreRinnovo: unknown) => {
            auth.esci();
            return throwError(() => erroreRinnovo);
          }),
          // Altrimenti si ripete la chiamata originale con il token nuovo
          switchMap(() => avanti(conToken(richiesta))),
        );
      }
      return throwError(() => errore);
    }),
  );
};
