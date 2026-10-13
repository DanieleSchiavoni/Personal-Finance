// src/app/models/pagina.model.ts
export interface Pagina<T> {
  contenuto: T[];
  pagina: number;
  dimensione: number;
  totaleElementi: number;
  totalePagine: number;
}
