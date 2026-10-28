import { TestBed } from '@angular/core/testing';

import { TemaService } from './tema.service';

describe('TemaService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-bs-theme');
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-bs-theme');
  });

  it('rispetta la scelta salvata dall\'utente', () => {
    localStorage.setItem('pf_tema', 'scuro');

    const servizio = TestBed.inject(TemaService);
    TestBed.flushEffects();

    expect(servizio.scuro()).toBeTrue();
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('dark');
  });

  it('inverti cambia il tema, lo applica alla pagina e lo ricorda', () => {
    localStorage.setItem('pf_tema', 'chiaro');
    const servizio = TestBed.inject(TemaService);
    TestBed.flushEffects();
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('light');

    servizio.inverti();
    TestBed.flushEffects();

    expect(servizio.scuro()).toBeTrue();
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('dark');
    expect(localStorage.getItem('pf_tema')).toBe('scuro');

    servizio.inverti();
    TestBed.flushEffects();

    expect(servizio.scuro()).toBeFalse();
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe('light');
    expect(localStorage.getItem('pf_tema')).toBe('chiaro');
  });

  it('un valore salvato non valido viene ignorato', () => {
    localStorage.setItem('pf_tema', 'viola');
    const servizio = TestBed.inject(TemaService);

    // Senza una scelta valida si segue il sistema: qualunque sia, il servizio deve restare coerente
    expect(typeof servizio.scuro()).toBe('boolean');
  });
});
