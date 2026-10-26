// src/app/core/guards/guardie.spec.ts
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { firstValueFrom, isObservable, of, throwError } from 'rxjs';

import { rispostaAutenticazione } from '../../testing/dati-di-prova';
import { AutenticazioneService } from '../services/autenticazione.service';
import { adminGuard } from './admin.guard';
import { autenticatoGuard } from './autenticato.guard';
import { homeGuard } from './home.guard';
import { ospiteGuard } from './ospite.guard';

interface Scenario {
  autenticato?: boolean;
  admin?: boolean;
  refresh?: boolean;
  rinnovoRiuscito?: boolean;
}

describe('Guardie delle rotte', () => {
  /** Al posto del vero servizio di sessione si usa un oggetto semplice, configurato dallo scenario */
  function preparaRouter(scenario: Scenario): Router {
    const finta = {
      autenticato: () => scenario.autenticato ?? false,
      isAdmin: () => scenario.admin ?? false,
      haRefreshToken: () => scenario.refresh ?? false,
      rinnova: () =>
        scenario.rinnovoRiuscito
          ? of(rispostaAutenticazione())
          : throwError(() => new Error('Rinnovo non riuscito')),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AutenticazioneService, useValue: finta }],
    });
    return TestBed.inject(Router);
  }

  async function esegui(guardia: CanActivateFn, url = '/transazioni'): Promise<boolean | UrlTree> {
    const esito = TestBed.runInInjectionContext(() =>
      guardia({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
    const valore = await (isObservable(esito) ? firstValueFrom(esito) : esito);
    return valore as boolean | UrlTree;
  }

  /** L'indirizzo verso cui la guardia rimanda, oppure un errore se non rimanda da nessuna parte */
  function destinazione(router: Router, esito: boolean | UrlTree): string {
    expect(esito instanceof UrlTree).toBeTrue();
    return router.serializeUrl(esito as UrlTree);
  }

  describe('autenticatoGuard', () => {
    it('lascia passare chi ha già una sessione', async () => {
      preparaRouter({ autenticato: true });

      expect(await esegui(autenticatoGuard)).toBeTrue();
    });

    it('senza sessione né refresh token rimanda al login ricordando la pagina richiesta', async () => {
      const router = preparaRouter({});

      const indirizzo = destinazione(router, await esegui(autenticatoGuard, '/transazioni'));

      expect(indirizzo).toContain('/login');
      expect(indirizzo).toContain('transazioni');
    });

    it('con un refresh token valido ripristina la sessione e lascia passare', async () => {
      preparaRouter({ refresh: true, rinnovoRiuscito: true });

      expect(await esegui(autenticatoGuard)).toBeTrue();
    });

    it('con un refresh token non più valido rimanda al login', async () => {
      const router = preparaRouter({ refresh: true, rinnovoRiuscito: false });

      expect(destinazione(router, await esegui(autenticatoGuard))).toContain('/login');
    });
  });

  describe('ospiteGuard', () => {
    it('lascia passare chi non è autenticato', async () => {
      preparaRouter({});

      expect(await esegui(ospiteGuard)).toBeTrue();
    });

    it('manda alla dashboard chi è già autenticato', async () => {
      const router = preparaRouter({ autenticato: true });

      expect(destinazione(router, await esegui(ospiteGuard))).toBe('/dashboard');
    });
  });

  describe('adminGuard', () => {
    it('lascia passare un amministratore', async () => {
      preparaRouter({ admin: true });

      expect(await esegui(adminGuard)).toBeTrue();
    });

    it('manda alla dashboard chi non è amministratore', async () => {
      const router = preparaRouter({ autenticato: true, admin: false });

      expect(destinazione(router, await esegui(adminGuard))).toBe('/dashboard');
    });
  });

  describe('homeGuard', () => {
    it('manda alla dashboard chi è già autenticato', async () => {
      const router = preparaRouter({ autenticato: true });

      expect(destinazione(router, await esegui(homeGuard))).toBe('/dashboard');
    });

    it('mostra la home a chi non ha nessuna sessione', async () => {
      preparaRouter({});

      expect(await esegui(homeGuard)).toBeTrue();
    });

    it('manda alla dashboard chi può ripristinare la sessione con il refresh token', async () => {
      const router = preparaRouter({ refresh: true, rinnovoRiuscito: true });

      expect(destinazione(router, await esegui(homeGuard))).toBe('/dashboard');
    });

    it('mostra la home se il refresh token non è più valido', async () => {
      preparaRouter({ refresh: true, rinnovoRiuscito: false });

      expect(await esegui(homeGuard)).toBeTrue();
    });
  });
});
