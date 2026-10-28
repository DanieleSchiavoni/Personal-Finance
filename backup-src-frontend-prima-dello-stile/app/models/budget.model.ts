// src/app/models/budget.model.ts
export type StatoBudget = 'OK' | 'ATTENZIONE' | 'SUPERATO';

export interface Budget {
  id: number;
  categoriaId: number;
  categoriaNome: string;
  categoriaColore: string | null;
  mese: number;
  anno: number;
  importo: number;
  speso: number;
  /** Diventa negativo se il budget è stato superato */
  disponibile: number;
  percentuale: number;
  stato: StatoBudget;
}

export interface BudgetRequest {
  categoriaId: number;
  importo: number;
  mese: number;
  anno: number;
}

/** Di un budget esistente si modifica solo l'importo */
export interface AggiornaBudgetRequest {
  importo: number;
}
