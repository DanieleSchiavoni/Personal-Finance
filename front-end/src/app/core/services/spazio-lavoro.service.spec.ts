// src/app/core/services/spazio-lavoro.service.spec.ts
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { spazioDiProva } from '../../testing/dati-di-prova';
import { SpazioLavoroService } from './spazio-lavoro.service';

describe('SpazioLavoroService', () => {
  const url = `${environment.apiUrl}/spazi-lavoro`;
  let servizio: SpazioLavoroService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servizio = TestBed.inject(SpazioLavoroService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carica seleziona l\'ultimo spazio usato', () => {
    localStorage.setItem('pf_spazio_attivo', '11');

    servizio.carica().subscribe();
    http.expectOne(url).flush([spazioDiProva({ id: 10 }), spazioDiProva({ id: 11, tipo: 'AZIENDALE', nome: 'Acme' })]);

    expect(servizio.spazioAttivoId()).toBe(11);
    expect(servizio.spazioAttivo()?.nome).toBe('Acme');
  });

  it('senza uno spazio salvato si sceglie il primo, che è quello personale', () => {
    servizio.carica().subscribe();
    http.expectOne(url).flush([spazioDiProva({ id: 10 }), spazioDiProva({ id: 11, tipo: 'AZIENDALE' })]);

    expect(servizio.spazioAttivoId()).toBe(10);
  });

  it('i permessi dipendono dal ruolo nello spazio attivo', () => {
    const verifica = (ruolo: 'PROPRIETARIO' | 'AMMINISTRATORE' | 'MEMBRO' | 'LETTORE') => {
      servizio.elenco.set([spazioDiProva({ ruoloUtente: ruolo })]);
      servizio.seleziona(10);
      return { gestire: servizio.puoGestire(), scrivere: servizio.puoScrivere() };
    };

    expect(verifica('PROPRIETARIO')).toEqual({ gestire: true, scrivere: true });
    expect(verifica('AMMINISTRATORE')).toEqual({ gestire: true, scrivere: true });
    expect(verifica('MEMBRO')).toEqual({ gestire: false, scrivere: true });
    expect(verifica('LETTORE')).toEqual({ gestire: false, scrivere: false });
  });

  it('creare uno spazio aziendale lo aggiunge all\'elenco e lo seleziona', () => {
    servizio.elenco.set([spazioDiProva({ id: 10 })]);

    servizio.creaAziendale('Acme S.r.l.').subscribe();
    const richiesta = http.expectOne(url);
    expect(richiesta.request.method).toBe('POST');
    expect(richiesta.request.body).toEqual({ nome: 'Acme S.r.l.' });
    richiesta.flush(spazioDiProva({ id: 12, nome: 'Acme S.r.l.', tipo: 'AZIENDALE' }));

    expect(servizio.elenco().length).toBe(2);
    expect(servizio.spazioAttivoId()).toBe(12);
  });

  it('reset svuota gli spazi e lo spazio attivo', () => {
    servizio.elenco.set([spazioDiProva()]);
    servizio.seleziona(10);

    servizio.reset();

    expect(servizio.elenco()).toEqual([]);
    expect(servizio.spazioAttivoId()).toBeNull();
  });
});
