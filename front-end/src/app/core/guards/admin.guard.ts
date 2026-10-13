// src/app/core/guards/admin.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AutenticazioneService } from '../services/autenticazione.service';

/** Solo gli amministratori. Il backend controlla comunque: questa guardia evita solo pagine inutili. */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AutenticazioneService);
  const router = inject(Router);

  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard']);
};
