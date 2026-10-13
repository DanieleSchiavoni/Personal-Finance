// src/app/models/spazio-lavoro.model.ts
export type TipoSpazio = 'PERSONALE' | 'AZIENDALE';
export type RuoloSpazio = 'PROPRIETARIO' | 'AMMINISTRATORE' | 'MEMBRO' | 'LETTORE';

export interface SpazioLavoro {
  id: number;
  nome: string;
  tipo: TipoSpazio;
  valuta: string;
  /** Il ruolo che l'utente ha in questo spazio */
  ruoloUtente: RuoloSpazio;
  dataCreazione: string;
}

export interface CreaSpazioLavoroRequest {
  nome: string;
  valuta?: string;
}
