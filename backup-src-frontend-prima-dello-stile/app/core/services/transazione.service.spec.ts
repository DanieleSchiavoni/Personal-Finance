// src/app/core/services/transazione.service.spec.ts
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { TransazioneService } from './transazione.service';

describe('TransazioneService', () => {
  let servizio: TransazioneService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servizio = TestBed.inject(TransazioneService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('la ricerca invia come parametri solo i filtri valorizzati', () => {
    servizio
      .cerca(5, { tipo: 'USCITA', testo: '', categoriaId: undefined, pagina: 0, dimensione: 10, importoMin: 0 })
      .subscribe();

    const richiesta = http.expectOne((r) => r.url === `${environment.apiUrl}/spazi-lavoro/5/transazioni`);
    expect(richiesta.request.method).toBe('GET');
    expect(richiesta.request.params.get('tipo')).toBe('USCITA');
    expect(richiesta.request.params.get('pagina')).toBe('0'); // zero è un valore valido
    expect(richiesta.request.params.get('importoMin')).toBe('0');
    expect(richiesta.request.params.has('testo')).toBeFalse(); // vuoto: non si invia
    expect(richiesta.request.params.has('categoriaId')).toBeFalse(); // non definito: non si invia
    richiesta.flush({ contenuto: [], pagina: 0, dimensione: 10, totaleElementi: 0, totalePagine: 0 });
  });
});
