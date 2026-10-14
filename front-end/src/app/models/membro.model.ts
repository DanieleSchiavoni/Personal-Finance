// src/app/models/membro.model.ts
import { RuoloSpazio } from './spazio-lavoro.model';

export interface Membro {
  utenteId: number;
  username: string;
  email: string;
  nome: string;
  cognome: string;
  ruolo: RuoloSpazio;
  dataIngresso: string;
}

export interface AggiungiMembroRequest {
  email: string;
  ruolo: RuoloSpazio;
}

export interface CambiaRuoloMembroRequest {
  ruolo: RuoloSpazio;
}

export const ETICHETTE_RUOLO: Record<RuoloSpazio, string> = {
  PROPRIETARIO: 'Proprietario',
  AMMINISTRATORE: 'Amministratore',
  MEMBRO: 'Membro',
  LETTORE: 'Lettore',
};
