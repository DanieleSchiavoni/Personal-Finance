// src/app/models/errore.model.ts
export interface ErroreCampo {
  campo: string;
  messaggio: string;
}

/** Risposta standard di errore del backend */
export interface ErroreApi {
  timestamp: string;
  status: number;
  errore: string;
  messaggio: string;
  percorso: string;
  campi?: ErroreCampo[];
}
