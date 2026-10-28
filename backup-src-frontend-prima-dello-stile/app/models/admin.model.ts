// src/app/models/admin.model.ts
import { TipoSpazio } from './spazio-lavoro.model';
import { TipoTransazione } from './transazione.model';

/** Solo conteggi: gli admin non vedono mai i dati finanziari degli utenti */
export interface StatisticheGenerali {
  utentiTotali: number;
  utentiAbilitati: number;
  utentiBloccati: number;
  amministratori: number;
  spaziPersonali: number;
  spaziAziendali: number;
  transazioniTotali: number;
  categorieGlobali: number;
}

export interface CategoriaGlobale {
  id: number;
  nome: string;
  tipo: TipoTransazione;
  colore: string | null;
  descrizione: string | null;
  /** A chi è visibile: null = a tutti i tipi di spazio */
  tipoSpazio: TipoSpazio | null;
}

export interface CategoriaGlobaleRequest {
  nome: string;
  tipo: TipoTransazione;
  colore: string | null;
  descrizione: string | null;
  tipoSpazio: TipoSpazio | null;
}

export interface RicercaUtenti {
  testo?: string;
  pagina?: number;
  dimensione?: number;
}

export interface ImpostaAbilitazioneRequest {
  abilitato: boolean;
}
