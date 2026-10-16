// src/app/core/services/budget.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Budget } from '../../models/budget.model';

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
}
