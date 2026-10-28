// src/app/core/services/notizie.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Notizia } from '../../models/notizia.model';

@Injectable({ providedIn: 'root' })
export class NotizieService {
  private readonly http = inject(HttpClient);

  /** Le ultime notizie di economia internazionale (endpoint pubblico) */
  ultime(limite = 24): Observable<Notizia[]> {
    return this.http.get<Notizia[]>(`${environment.apiUrl}/notizie`, { params: { limite } });
  }
}
