// src/app/core/services/transazione.service.ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Pagina } from '../../models/pagina.model';
import { RicercaTransazioni, Transazione, TransazioneRequest } from '../../models/transazione.model';

@Injectable({ providedIn: 'root' })
export class TransazioneService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/transazioni`;
  }

  cerca(spazioId: number, ricerca: RicercaTransazioni): Observable<Pagina<Transazione>> {
    // Solo i filtri valorizzati diventano parametri della richiesta
    let parametri = new HttpParams();
    for (const [chiave, valore] of Object.entries(ricerca)) {
      if (valore !== undefined && valore !== null && valore !== '') {
        parametri = parametri.set(chiave, String(valore));
      }
    }
    return this.http.get<Pagina<Transazione>>(this.url(spazioId), { params: parametri });
  }

  dettaglio(spazioId: number, id: number): Observable<Transazione> {
    return this.http.get<Transazione>(`${this.url(spazioId)}/${id}`);
  }

  crea(spazioId: number, richiesta: TransazioneRequest): Observable<Transazione> {
    return this.http.post<Transazione>(this.url(spazioId), richiesta);
  }

  aggiorna(spazioId: number, id: number, richiesta: TransazioneRequest): Observable<Transazione> {
    return this.http.put<Transazione>(`${this.url(spazioId)}/${id}`, richiesta);
  }

  elimina(spazioId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.url(spazioId)}/${id}`);
  }
}
