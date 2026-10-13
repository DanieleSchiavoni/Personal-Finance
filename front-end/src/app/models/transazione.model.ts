// src/app/models/transazione.model.ts
export type TipoTransazione = 'ENTRATA' | 'USCITA';
export type MetodoPagamento = 'CARTA' | 'CONTANTI' | 'BONIFICO' | 'ALTRO';

export const METODI_PAGAMENTO: { valore: MetodoPagamento; etichetta: string }[] = [
  { valore: 'CARTA', etichetta: 'Carta' },
  { valore: 'CONTANTI', etichetta: 'Contanti' },
  { valore: 'BONIFICO', etichetta: 'Bonifico' },
  { valore: 'ALTRO', etichetta: 'Altro' },
];

export interface Transazione {
  id: number;
  importo: number;
  descrizione: string;
  data: string;
  tipo: TipoTransazione;
  metodoPagamento: MetodoPagamento;
  note: string | null;
  categoriaId: number;
  categoriaNome: string;
  categoriaColore: string | null;
  creataDaId: number | null;
  creataDaUsername: string | null;
}

export interface TransazioneRequest {
  importo: number;
  descrizione: string;
  data: string;
  tipo: TipoTransazione;
  metodoPagamento: MetodoPagamento;
  categoriaId: number;
  note: string | null;
}

/** Filtri, paginazione e ordinamento della ricerca: tutti facoltativi */
export interface RicercaTransazioni {
  dal?: string;
  al?: string;
  categoriaId?: number;
  tipo?: TipoTransazione;
  metodoPagamento?: MetodoPagamento;
  importoMin?: number;
  importoMax?: number;
  testo?: string;
  pagina?: number;
  dimensione?: number;
  ordinaPer?: 'data' | 'importo' | 'descrizione' | 'categoria';
  direzione?: 'asc' | 'desc';
}
