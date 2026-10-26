// src/app/core/interceptors/auth.interceptor.spec.ts
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { rispostaAutenticazione } from '../../testing/dati-di-prova';
import { AutenticazioneService } from '../services/autenticazione.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  const urlApi = `${environment.apiUrl}/spazi-lavoro`;
  const urlAuth = `${environment.apiUrl}/auth`;
  let cliente: HttpClient;
  let http: HttpTestingController;
  let auth: AutenticazioneService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AutenticazioneService);

    // Una sessione già aperta, con access token "access-1"
    auth.login({ identificativo: 'mario', password: 'Password123' }).subscribe();
    http.expectOne(`${urlAuth}/login`).flush(rispostaAutenticazione());
  });

  afterEach(() => http.verify());

  it('aggiunge il token alle chiamate verso le nostre API', () => {
    cliente.get(urlApi).subscribe();

    const richiesta = http.expectOne(urlApi);
    expect(richiesta.request.headers.get('Authorization')).toBe('Bearer access-1');
    richiesta.flush([]);
  });

  it('non aggiunge il token alle chiamate di autenticazione', () => {
    cliente.post(`${urlAuth}/refresh`, {}).subscribe();

    const richiesta = http.expectOne(`${urlAuth}/refresh`);
    expect(richiesta.request.headers.has('Authorization')).toBeFalse();
    richiesta.flush({});
  });

  it('non invia mai il token a indirizzi esterni', () => {
    cliente.get('https://esempio.org/dati').subscribe();

    const richiesta = http.expectOne('https://esempio.org/dati');
    expect(richiesta.request.headers.has('Authorization')).toBeFalse();
    richiesta.flush({});
  });

  it('su un 401 rinnova il token e ripete la chiamata una sola volta', () => {
    let risposta: unknown;

    cliente.get(urlApi).subscribe((valore) => (risposta = valore));
    http.expectOne(urlApi).flush({ messaggio: 'Token scaduto' }, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${urlAuth}/refresh`).flush(rispostaAutenticazione('access-2', 'refresh-2'));

    const ripetuta = http.expectOne(urlApi);
    expect(ripetuta.request.headers.get('Authorization')).toBe('Bearer access-2');
    ripetuta.flush(['ok']);

    expect(risposta).toEqual(['ok']);
  });

  it('se il rinnovo fallisce l\'utente esce e l\'errore arriva a chi ha fatto la chiamata', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    let errore: unknown;

    cliente.get(urlApi).subscribe({ error: (e) => (errore = e) });
    http.expectOne(urlApi).flush({ messaggio: 'Token scaduto' }, { status: 401, statusText: 'Unauthorized' });
    http
      .expectOne(`${urlAuth}/refresh`)
      .flush({ messaggio: 'Refresh token non valido' }, { status: 401, statusText: 'Unauthorized' });

    expect(errore).toBeTruthy();
    expect(auth.autenticato()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('gli altri errori non provocano un rinnovo del token', () => {
    let stato: number | undefined;

    cliente.get(urlApi).subscribe({ error: (e) => (stato = e.status) });
    http.expectOne(urlApi).flush({ messaggio: 'Errore' }, { status: 500, statusText: 'Server Error' });

    expect(stato).toBe(500);
    http.expectNone(`${urlAuth}/refresh`);
  });
});
