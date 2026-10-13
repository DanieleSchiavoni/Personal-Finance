// src/app/models/categoria.model.ts
import { TipoTransazione } from './transazione.model';

export interface Categoria {
  id: number;
  nome: string;
  tipo: TipoTransazione;
  colore: string | null;
  descrizione: string | null;
  /** true per le categorie predefinite, che non si possono modificare */
  globale: boolean;
}

export interface CategoriaRequest {
  nome: string;
  tipo: TipoTransazione;
  colore: string | null;
  descrizione: string | null;
}
