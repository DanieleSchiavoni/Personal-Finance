// src/app/models/autenticazione.model.ts
import { Utente } from './utente.model';

export interface LoginRequest {
  /** Username oppure email */
  identificativo: string;
  password: string;
}

export interface RegistrazioneRequest {
  username: string;
  email: string;
  password: string;
  nome: string;
  cognome: string;
}

export interface RinnovaTokenRequest {
  refreshToken: string;
}

export interface AutenticazioneResponse {
  accessToken: string;
  refreshToken: string;
  tipoToken: string;
  scadenzaAccessTokenSecondi: number;
  utente: Utente;
}
