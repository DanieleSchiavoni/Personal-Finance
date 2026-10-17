import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AggiornaBudgetRequest, Budget, BudgetRequest } from '../../models/budget.model';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/budget`;
  }

  /** I budget di un mese, con speso, disponibile, percentuale e stato già calcolati dal backend */
  elenca(spazioId: number, mese: number, anno: number): Observable<Budget[]> {
    return this.http.get<Budget[]>(this.url(spazioId), { params: { mese, anno } });
  }

  crea(spazioId: number, richiesta: BudgetRequest): Observable<Budget> {
    return this.http.post<Budget>(this.url(spazioId), richiesta);
  }

  aggiorna(spazioId: number, id: number, richiesta: AggiornaBudgetRequest): Observable<Budget> {
    return this.http.patch<Budget>(`${this.url(spazioId)}/${id}`, richiesta);
  }

  elimina(spazioId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.url(spazioId)}/${id}`);
  }
}
