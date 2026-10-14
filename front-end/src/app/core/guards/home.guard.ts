// src/app/core/guards/home.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AutenticazioneService } from '../services/autenticazione.service';

/**
 * Per la home pubblica: chi ha già una sessione (o può ripristinarla con il refresh token)
 * va direttamente alla dashboard; chi non ha accesso vede la home.
 */
export const homeGuard: CanActivateFn = () => {
  const auth = inject(AutenticazioneService);
  const router = inject(Router);
  const versoDashboard = router.createUrlTree(['/dashboard']);

  if (auth.autenticato()) {
    return versoDashboard;
  }
  if (!auth.haRefreshToken()) {
    return true;
  }
  return auth.rinnova().pipe(
    map(() => versoDashboard),
    catchError(() => of(true)),
  );
};
