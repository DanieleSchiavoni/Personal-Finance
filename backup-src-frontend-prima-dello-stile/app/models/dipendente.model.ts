// src/app/models/dipendente.model.ts
import { MetodoPagamento } from './transazione.model';

export interface Dipendente {
  id: number;
  nome: string;
  cognome: string;
  mansione: string | null;
  stipendioMensile: number;
  dataAssunzione: string | null;
  attivo: boolean;
}

export interface DipendenteRequest {
  nome: string;
  cognome: string;
  mansione: string | null;
  stipendioMensile: number;
  dataAssunzione: string | null;
  attivo: boolean;
}

/** Un pagamento di stipendio già registrato */
export interface PagamentoStipendio {
  transazioneId: number;
  mese: number;
  anno: number;
  importo: number;
  dataPagamento: string;
}

export interface PagamentoStipendioRequest {
  mese: number;
  anno: number;
  dataPagamento: string;
  importo: number;
  metodoPagamento: MetodoPagamento;
}
