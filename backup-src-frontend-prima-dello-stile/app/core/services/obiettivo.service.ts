// src/app/core/services/obiettivo.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ObiettivoRequest, ObiettivoRisparmio, VersamentoRequest } from '../../models/obiettivo.model';

@Injectable({ providedIn: 'root' })
export class ObiettivoService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/obiettivi`;
  }

  elenca(spazioId: number): Observable<ObiettivoRisparmio[]> {
    return this.http.get<ObiettivoRisparmio[]>(this.url(spazioId));
  }

  crea(spazioId: number, richiesta: ObiettivoRequest): Observable<ObiettivoRisparmio> {
    return this.http.post<ObiettivoRisparmio>(this.url(spazioId), richiesta);
  }

  aggiorna(spazioId: number, id: number, richiesta: ObiettivoRequest): Observable<ObiettivoRisparmio> {
    return this.http.put<ObiettivoRisparmio>(`${this.url(spazioId)}/${id}`, richiesta);
  }

  elimina(spazioId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.url(spazioId)}/${id}`);
  }

  /** Aggiunge denaro all'obiettivo e restituisce l'obiettivo aggiornato */
  versa(spazioId: number, id: number, importo: number): Observable<ObiettivoRisparmio> {
    const richiesta: VersamentoRequest = { importo };
    return this.http.patch<ObiettivoRisparmio>(`${this.url(spazioId)}/${id}/versamenti`, richiesta);
  }
}
