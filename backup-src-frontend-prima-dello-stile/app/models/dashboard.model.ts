// src/app/models/dashboard.model.ts
import { Transazione } from './transazione.model';

export interface TotaleCategoria {
  categoriaId: number;
  categoriaNome: string;
  colore: string | null;
  totale: number;
  /** Quota della categoria sul totale delle uscite del periodo */
  percentuale: number;
}

/** speso = quanto speso nelle sole categorie che hanno un budget nel mese */
export interface SintesiBudget {
  totale: number;
  speso: number;
  percentuale: number;
}

export interface SintesiObiettivi {
  totali: number;
  completati: number;
  importoRisparmiato: number;
  importoObiettivi: number;
}

export interface RiepilogoDashboard {
  mese: number;
  anno: number;
  saldoTotale: number;
  entrateMese: number;
  usciteMese: number;
  risparmioMese: number;
  transazioniMese: number;
  budget: SintesiBudget;
  obiettivi: SintesiObiettivi;
  categorieConPiuSpesa: TotaleCategoria[];
  ultimeTransazioni: Transazione[];
}

export type Granularita = 'SETTIMANA' | 'MESE';

/** Un periodo (settimana o mese) del grafico di andamento */
export interface PuntoTrend {
  etichetta: string;
  inizio: string;
  fine: string;
  entrate: number;
  uscite: number;
  saldoPeriodo: number;
  /** Il saldo complessivo a fine periodo */
  saldoProgressivo: number;
}
