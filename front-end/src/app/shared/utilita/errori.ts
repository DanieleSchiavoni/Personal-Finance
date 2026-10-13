// src/app/shared/utilita/errori.ts
import { HttpErrorResponse } from '@angular/common/http';

import { ErroreApi } from '../../models/errore.model';

/** Se l'errore arriva dal nostro backend restituisce la risposta standard, altrimenti null */
export function leggiErrore(errore: unknown): ErroreApi | null {
  if (
    errore instanceof HttpErrorResponse &&
    errore.error &&
    typeof errore.error === 'object' &&
    'messaggio' in errore.error
  ) {
    return errore.error as ErroreApi;
  }
  return null;
}

/** Un messaggio leggibile da mostrare all'utente, con i dettagli dei campi sbagliati */
export function messaggioErrore(
  errore: unknown,
  predefinito = 'Si è verificato un errore. Riprova tra poco.',
): string {
  const erroreApi = leggiErrore(errore);
  if (erroreApi) {
    const dettagli = erroreApi.campi?.map((campo) => campo.messaggio).join(' ');
    return dettagli ? `${erroreApi.messaggio}: ${dettagli}` : erroreApi.messaggio;
  }
  if (errore instanceof HttpErrorResponse && errore.status === 0) {
    return 'Server non raggiungibile. Controlla che il backend sia avviato.';
  }
  return predefinito;
}
