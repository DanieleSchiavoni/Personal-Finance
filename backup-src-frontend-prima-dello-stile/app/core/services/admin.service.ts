// src/app/core/services/admin.service.ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CategoriaGlobale,
  CategoriaGlobaleRequest,
  ImpostaAbilitazioneRequest,
  RicercaUtenti,
  StatisticheGenerali,
} from '../../models/admin.model';
import { Pagina } from '../../models/pagina.model';
import { Utente } from '../../models/utente.model';

/** Chiamate riservate agli amministratori */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin`;

  statistiche(): Observable<StatisticheGenerali> {
    return this.http.get<StatisticheGenerali>(`${this.url}/statistiche`);
  }

  elencaUtenti(ricerca: RicercaUtenti): Observable<Pagina<Utente>> {
    let parametri = new HttpParams();
    for (const [chiave, valore] of Object.entries(ricerca)) {
      if (valore !== undefined && valore !== null && valore !== '') {
        parametri = parametri.set(chiave, String(valore));
      }
    }
    return this.http.get<Pagina<Utente>>(`${this.url}/utenti`, { params: parametri });
  }

  /** Bloccando un utente il backend revoca anche le sue sessioni */
  impostaAbilitazione(utenteId: number, abilitato: boolean): Observable<Utente> {
    const richiesta: ImpostaAbilitazioneRequest = { abilitato };
    return this.http.patch<Utente>(`${this.url}/utenti/${utenteId}/abilitazione`, richiesta);
  }

  elencaCategorie(): Observable<CategoriaGlobale[]> {
    return this.http.get<CategoriaGlobale[]>(`${this.url}/categorie`);
  }

  creaCategoria(richiesta: CategoriaGlobaleRequest): Observable<CategoriaGlobale> {
    return this.http.post<CategoriaGlobale>(`${this.url}/categorie`, richiesta);
  }

  aggiornaCategoria(id: number, richiesta: CategoriaGlobaleRequest): Observable<CategoriaGlobale> {
    return this.http.put<CategoriaGlobale>(`${this.url}/categorie/${id}`, richiesta);
  }

  eliminaCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/categorie/${id}`);
  }
}
