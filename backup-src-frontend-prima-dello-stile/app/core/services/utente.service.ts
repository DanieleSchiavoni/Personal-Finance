// src/app/core/services/utente.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AggiornaProfiloRequest,
  CambiaPasswordRequest,
  ConfermaPasswordRequest,
} from '../../models/profilo.model';
import { Utente } from '../../models/utente.model';

@Injectable({ providedIn: 'root' })
export class UtenteService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/utenti/me`;

  aggiorna(richiesta: AggiornaProfiloRequest): Observable<Utente> {
    return this.http.put<Utente>(this.url, richiesta);
  }

  cambiaPassword(richiesta: CambiaPasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.url}/password`, richiesta);
  }

  /** Il backend richiede la password come conferma */
  eliminaAccount(password: string): Observable<void> {
    const richiesta: ConfermaPasswordRequest = { password };
    return this.http.post<void>(`${this.url}/eliminazione`, richiesta);
  }
}
