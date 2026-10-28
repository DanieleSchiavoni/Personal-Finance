// src/app/models/utente.model.ts
export type RuoloUtente = 'UTENTE' | 'ADMIN';

export interface Utente {
  id: number;
  username: string;
  email: string;
  nome: string;
  cognome: string;
  ruolo: RuoloUtente;
  abilitato: boolean;
  dataCreazione: string;
}
