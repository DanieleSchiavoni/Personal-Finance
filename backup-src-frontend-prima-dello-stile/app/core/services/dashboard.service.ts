import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Granularita, PuntoTrend, RiepilogoDashboard, TotaleCategoria } from '../../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/dashboard`;
  }

  /** Le schede principali della dashboard per un mese */
  riepilogo(spazioId: number, mese: number, anno: number): Observable<RiepilogoDashboard> {
    return this.http.get<RiepilogoDashboard>(`${this.url(spazioId)}/riepilogo`, { params: { mese, anno } });
  }

  /** Entrate, uscite e saldo per settimana o per mese, da "dal" fino a oggi */
  trend(spazioId: number, granularita: Granularita, dal: string): Observable<PuntoTrend[]> {
    return this.http.get<PuntoTrend[]>(`${this.url(spazioId)}/trend`, { params: { granularita, dal } });
  }

  /** Le uscite del periodo, divise per categoria */
  spesePerCategoria(spazioId: number, dal: string, al: string): Observable<TotaleCategoria[]> {
    return this.http.get<TotaleCategoria[]>(`${this.url(spazioId)}/spese-per-categoria`, {
      params: { dal, al },
    });
  }
}
