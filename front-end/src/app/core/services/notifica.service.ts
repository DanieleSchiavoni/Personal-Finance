// src/app/core/services/notifica.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConteggioNotifiche, Notifica } from '../../models/notifica.model';

@Injectable({ providedIn: 'root' })
export class NotificaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/notifiche`;

  /** Condiviso tra il badge nella sidebar e la pagina delle notifiche */
  readonly nonLette = signal(0);

  elenca(): Observable<Notifica[]> {
    return this.http.get<Notifica[]>(this.url).pipe(
      tap((notifiche) => this.nonLette.set(notifiche.filter((n) => !n.letta).length)),
    );
  }

  aggiornaConteggio(): Observable<number> {
    return this.http.get<ConteggioNotifiche>(`${this.url}/conteggio-non-lette`).pipe(
      map((risposta) => risposta.nonLette),
      tap((conteggio) => this.nonLette.set(conteggio)),
    );
  }

  segnaComeLetta(id: number): Observable<Notifica> {
    return this.http.patch<Notifica>(`${this.url}/${id}/letta`, {});
  }

  segnaTutteComeLette(): Observable<void> {
    return this.http.patch<void>(`${this.url}/segna-tutte-come-lette`, {});
  }

  elimina(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
