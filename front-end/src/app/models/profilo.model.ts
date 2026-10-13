// src/app/models/profilo.model.ts
export interface AggiornaProfiloRequest {
  username: string;
  email: string;
  nome: string;
  cognome: string;
}

export interface CambiaPasswordRequest {
  passwordAttuale: string;
  nuovaPassword: string;
}

export interface ConfermaPasswordRequest {
  password: string;
}
