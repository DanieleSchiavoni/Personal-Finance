// src/app/models/notizia.model.ts
export interface Notizia {
  titolo: string;
  sintesi: string;
  url: string;
  fonte: string;
  lingua: string;
  immagineUrl: string | null;
  pubblicataIl: string | null;
}
