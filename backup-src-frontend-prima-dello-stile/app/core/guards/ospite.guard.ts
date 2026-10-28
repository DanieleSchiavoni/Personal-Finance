// src/app/core/guards/ospite.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AutenticazioneService } from '../services/autenticazione.service';

/** Per login e registrazione: chi ha già una sessione va direttamente alla dashboard */
export const ospiteGuard: CanActivateFn = () => {
  const auth = inject(AutenticazioneService);
  const router = inject(Router);

  return auth.autenticato() ? router.createUrlTree(['/dashboard']) : true;
};
