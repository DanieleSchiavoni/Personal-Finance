// src/app/shared/utilita/validatori.spec.ts
import { FormControl, FormGroup } from '@angular/forms';

import { PATTERN_PASSWORD, campiUguali } from './validatori';

describe('validatori', () => {
  it('la password richiede almeno una lettera e un numero', () => {
    expect(PATTERN_PASSWORD.test('Password123')).toBeTrue();
    expect(PATTERN_PASSWORD.test('soloLettere')).toBeFalse();
    expect(PATTERN_PASSWORD.test('12345678')).toBeFalse();
  });

  it('campiUguali non segnala errori se i due campi coincidono', () => {
    const gruppo = new FormGroup({ password: new FormControl('abc123'), conferma: new FormControl('abc123') });

    expect(campiUguali('password', 'conferma')(gruppo)).toBeNull();
  });

  it('campiUguali segnala l\'errore se i due campi sono diversi', () => {
    const gruppo = new FormGroup({ password: new FormControl('abc123'), conferma: new FormControl('abc124') });

    expect(campiUguali('password', 'conferma')(gruppo)).toEqual({ nonCoincidono: true });
  });
});
