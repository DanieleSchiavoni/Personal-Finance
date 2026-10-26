// src/app/core/services/autenticazione.service.spec.ts
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { rispostaAutenticazione } from '../../testing/dati-di-prova';
import { AutenticazioneService } from './autenticazione.service';

describe('AutenticazioneService', () => {
  const urlAuth = `${environment.apiUrl}/auth`;
  let servizio: AutenticazioneService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servizio = TestBed.inject(AutenticazioneService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function accedi(): void {
    servizio.login({ identificativo: 'mario', password: 'Password123' }).subscribe();
    http.expectOne(`${urlAuth}/login`).flush(rispostaAutenticazione());
  }

  it('il login salva la sessione', () => {
    servizio.login({ identificativo: 'mario', password: 'Password123' }).subscribe();

    const richiesta = http.expectOne(`${urlAuth}/login`);
    expect(richiesta.request.method).toBe('POST');
    richiesta.flush(rispostaAutenticazione());

    expect(servizio.autenticato()).toBeTrue();
    expect(servizio.utente()?.username).toBe('mario');
    expect(servizio.getAccessToken()).toBe('access-1');
    expect(servizio.haRefreshToken()).toBeTrue();
  });

  it('senza refresh token il rinnovo fallisce subito, senza chiamare il backend', () => {
    let errore: unknown;

    servizio.rinnova().subscribe({ error: (e) => (errore = e) });

    expect(errore).toBeTruthy();
    http.expectNone(`${urlAuth}/refresh`);
  });

  it('più rinnovi contemporanei fanno una sola richiesta', () => {
    accedi();
    const risultati: string[] = [];

    servizio.rinnova().subscribe((risposta) => risultati.push(risposta.accessToken));
    servizio.rinnova().subscribe((risposta) => risultati.push(risposta.accessToken));

    // expectOne fallirebbe se le richieste fossero due
    http.expectOne(`${urlAuth}/refresh`).flush(rispostaAutenticazione('access-2', 'refresh-2'));

    expect(risultati).toEqual(['access-2', 'access-2']);
    expect(servizio.getAccessToken()).toBe('access-2');
  });

  it('se il refresh token non è più valido (401) la sessione viene cancellata', () => {
    accedi();
    let errore: unknown;

    servizio.rinnova().subscribe({ error: (e) => (errore = e) });
    http
      .expectOne(`${urlAuth}/refresh`)
      .flush({ messaggio: 'Refresh token non valido' }, { status: 401, statusText: 'Unauthorized' });

    expect(errore).toBeTruthy();
    expect(servizio.autenticato()).toBeFalse();
    expect(servizio.haRefreshToken()).toBeFalse();
  });

  it('un errore del server (500) durante il rinnovo non cancella la sessione', () => {
    accedi();

    servizio.rinnova().subscribe({ error: () => undefined });
    http
      .expectOne(`${urlAuth}/refresh`)
      .flush({ messaggio: 'Errore' }, { status: 500, statusText: 'Server Error' });

    expect(servizio.autenticato()).toBeTrue();
    expect(servizio.haRefreshToken()).toBeTrue();
  });

  it('esci cancella la sessione, avvisa il backend e torna al login', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    accedi();

    servizio.esci();

    const logout = http.expectOne(`${urlAuth}/logout`);
    expect(logout.request.body).toEqual({ refreshToken: 'refresh-1' });
    logout.flush(null);

    expect(servizio.autenticato()).toBeFalse();
    expect(servizio.getAccessToken()).toBeNull();
    expect(servizio.haRefreshToken()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
