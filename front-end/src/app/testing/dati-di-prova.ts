// src/app/testing/dati-di-prova.ts
import { AutenticazioneResponse } from '../models/autenticazione.model';
import { SpazioLavoro } from '../models/spazio-lavoro.model';
import { Utente } from '../models/utente.model';

export const UTENTE_DI_PROVA: Utente = {
  id: 1,
  username: 'mario',
  email: 'mario@test.it',
  nome: 'Mario',
  cognome: 'Rossi',
  ruolo: 'UTENTE',
  abilitato: true,
  dataCreazione: '2026-10-01T10:00:00',
};

/** La risposta del backend a login, registrazione e rinnovo */
export function rispostaAutenticazione(
  accessToken = 'access-1',
  refreshToken = 'refresh-1',
  utente: Utente = UTENTE_DI_PROVA,
): AutenticazioneResponse {
  return { accessToken, refreshToken, tipoToken: 'Bearer', scadenzaAccessTokenSecondi: 900, utente };
}

export function spazioDiProva(sovrascritture: Partial<SpazioLavoro> = {}): SpazioLavoro {
  return {
    id: 10,
    nome: 'Personale',
    tipo: 'PERSONALE',
    valuta: 'EUR',
    ruoloUtente: 'PROPRIETARIO',
    dataCreazione: '2026-10-01T10:00:00',
    ...sovrascritture,
  };
}
