// src/app/core/guards/autenticato.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AutenticazioneService } from '../services/autenticazione.service';

/**
 * Lascia passare solo chi ha una sessione. Dopo un ricaricamento della pagina l'access token non c'è più:
 * se esiste un refresh token se ne ottiene uno nuovo, altrimenti si va al login.
 */
export const autenticatoGuard: CanActivateFn = (_route, stato) => {
  const auth = inject(AutenticazioneService);
  const router = inject(Router);

  if (auth.autenticato()) {
    return true;
  }

  const versoLogin = router.createUrlTree(['/login'], { queryParams: { indirizzo: stato.url } });
  if (!auth.haRefreshToken()) {
    return versoLogin;
  }
  return auth.rinnova().pipe(
    map(() => true),
    catchError(() => of(versoLogin)),
  );
};
