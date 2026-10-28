// src/app/models/notifica.model.ts
export interface Notifica {
  id: number;
  titolo: string;
  messaggio: string;
  data: string;
  letta: boolean;
}

export interface ConteggioNotifiche {
  nonLette: number;
}
