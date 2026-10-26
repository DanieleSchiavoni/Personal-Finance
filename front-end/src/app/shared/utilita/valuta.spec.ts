// src/app/shared/utilita/valuta.spec.ts
import { formattaValuta } from './valuta';

describe('formattaValuta', () => {
  it('scrive gli importi all\'italiana, con il simbolo della valuta', () => {
    // Il formato usa uno spazio "non separabile" prima del simbolo: lo si porta a uno spazio normale
    const testo = formattaValuta(12345.6, 'EUR').replace(/\u00a0/g, ' ');

    expect(testo).toBe('12.345,60 €');
  });

  it('con un codice valuta non valido scrive comunque l\'importo', () => {
    expect(formattaValuta(10, 'EUROX')).toBe('10.00 EUROX');
  });
});
