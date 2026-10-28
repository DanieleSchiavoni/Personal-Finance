// src/app/shared/utilita/validatori.ts
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Password con almeno una lettera e un numero (come nel backend) */
export const PATTERN_PASSWORD = /^(?=.*[A-Za-z])(?=.*\d).+$/;

/** Validatore di gruppo: due campi devono avere lo stesso valore (ad esempio password e conferma) */
export function campiUguali(campo: string, campoConferma: string): ValidatorFn {
  return (gruppo: AbstractControl): ValidationErrors | null =>
    gruppo.get(campo)?.value === gruppo.get(campoConferma)?.value ? null : { nonCoincidono: true };
}
