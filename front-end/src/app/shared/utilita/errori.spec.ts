// src/app/shared/utilita/errori.spec.ts
import { HttpErrorResponse } from '@angular/common/http';

import { leggiErrore, messaggioErrore } from './errori';

describe('errori', () => {
  const erroreDelBackend = (campi?: { campo: string; messaggio: string }[]) =>
    new HttpErrorResponse({
      status: 400,
      error: {
        timestamp: '2026-10-15T10:00:00',
        status: 400,
        errore: 'ERRORE_VALIDAZIONE',
        messaggio: 'Dati non validi',
        percorso: '/api/transazioni',
        campi,
      },
    });

  describe('leggiErrore', () => {
    it('restituisce la risposta standard del backend', () => {
      expect(leggiErrore(erroreDelBackend())?.errore).toBe('ERRORE_VALIDAZIONE');
    });

    it('restituisce null per gli errori che non vengono dal backend', () => {
      expect(leggiErrore(new Error('qualcosa'))).toBeNull();
      expect(leggiErrore(new HttpErrorResponse({ status: 500, error: 'testo semplice' }))).toBeNull();
    });
  });

  describe('messaggioErrore', () => {
    it('aggiunge al messaggio i dettagli dei campi sbagliati', () => {
      const errore = erroreDelBackend([
        { campo: 'importo', messaggio: "L'importo deve essere maggiore di zero" },
        { campo: 'descrizione', messaggio: 'La descrizione è obbligatoria' },
      ]);

      expect(messaggioErrore(errore)).toBe(
        "Dati non validi: L'importo deve essere maggiore di zero La descrizione è obbligatoria",
      );
    });

    it('usa solo il messaggio quando non ci sono campi', () => {
      expect(messaggioErrore(erroreDelBackend())).toBe('Dati non validi');
    });

    it('spiega quando il server non è raggiungibile', () => {
      expect(messaggioErrore(new HttpErrorResponse({ status: 0 }))).toContain('non raggiungibile');
    });

    it('per un errore sconosciuto usa il messaggio predefinito, anche personalizzato', () => {
      expect(messaggioErrore(new Error('boh'))).toContain('Si è verificato un errore');
      expect(messaggioErrore(new Error('boh'), 'Operazione non riuscita')).toBe('Operazione non riuscita');
    });
  });
});
