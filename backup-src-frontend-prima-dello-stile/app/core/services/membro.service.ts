// src/app/core/services/membro.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AggiungiMembroRequest, CambiaRuoloMembroRequest, Membro } from '../../models/membro.model';
import { RuoloSpazio } from '../../models/spazio-lavoro.model';

@Injectable({ providedIn: 'root' })
export class MembroService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/membri`;
  }

  elenca(spazioId: number): Observable<Membro[]> {
    return this.http.get<Membro[]>(this.url(spazioId));
  }

  /** Aggiunge un utente già registrato, indicato per email */
  aggiungi(spazioId: number, richiesta: AggiungiMembroRequest): Observable<Membro> {
    return this.http.post<Membro>(this.url(spazioId), richiesta);
  }

  cambiaRuolo(spazioId: number, utenteId: number, ruolo: RuoloSpazio): Observable<Membro> {
    const richiesta: CambiaRuoloMembroRequest = { ruolo };
    return this.http.patch<Membro>(`${this.url(spazioId)}/${utenteId}`, richiesta);
  }

  /** Rimuove un membro; se è l'utente stesso, è un'uscita dallo spazio */
  rimuovi(spazioId: number, utenteId: number): Observable<void> {
    return this.http.delete<void>(`${this.url(spazioId)}/${utenteId}`);
  }
}
