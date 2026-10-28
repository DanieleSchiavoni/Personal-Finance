// src/app/core/services/dipendente.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Dipendente,
  DipendenteRequest,
  PagamentoStipendio,
  PagamentoStipendioRequest,
} from '../../models/dipendente.model';
import { Transazione } from '../../models/transazione.model';

/** Solo PROPRIETARIO e AMMINISTRATORE di uno spazio aziendale: gli altri ricevono 403 */
@Injectable({ providedIn: 'root' })
export class DipendenteService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/dipendenti`;
  }

  elenca(spazioId: number): Observable<Dipendente[]> {
    return this.http.get<Dipendente[]>(this.url(spazioId));
  }

  crea(spazioId: number, richiesta: DipendenteRequest): Observable<Dipendente> {
    return this.http.post<Dipendente>(this.url(spazioId), richiesta);
  }

  aggiorna(spazioId: number, id: number, richiesta: DipendenteRequest): Observable<Dipendente> {
    return this.http.put<Dipendente>(`${this.url(spazioId)}/${id}`, richiesta);
  }

  /** Solo se il dipendente non ha pagamenti registrati (altrimenti 409: va disattivato) */
  elimina(spazioId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.url(spazioId)}/${id}`);
  }

  /** Registra lo stipendio di un mese: il backend crea la transazione di uscita collegata */
  registraPagamento(spazioId: number, id: number, richiesta: PagamentoStipendioRequest): Observable<Transazione> {
    return this.http.post<Transazione>(`${this.url(spazioId)}/${id}/pagamenti-stipendio`, richiesta);
  }

  /** I pagamenti già registrati, dal più recente */
  elencaPagamenti(spazioId: number, id: number): Observable<PagamentoStipendio[]> {
    return this.http.get<PagamentoStipendio[]>(`${this.url(spazioId)}/${id}/pagamenti-stipendio`);
  }
}
