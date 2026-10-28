// src/app/models/obiettivo.model.ts
export type StatoObiettivo = 'IN_CORSO' | 'COMPLETATO' | 'SCADUTO';

export interface ObiettivoRisparmio {
  id: number;
  nome: string;
  descrizione: string | null;
  importoObiettivo: number;
  importoAttuale: number;
  importoMancante: number;
  percentuale: number;
  scadenza: string | null;
  stato: StatoObiettivo;
}

export interface ObiettivoRequest {
  nome: string;
  descrizione: string | null;
  importoObiettivo: number;
  scadenza: string | null;
}

export interface VersamentoRequest {
  importo: number;
}
