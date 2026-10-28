// src/app/core/services/report.service.ts
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, from, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  /** Il PDF del resoconto di un mese (solo PROPRIETARIO e AMMINISTRATORE) */
  scaricaResocontoMensile(spazioId: number, mese: number, anno: number): Observable<Blob> {
    return this.http
      .get(`${environment.apiUrl}/spazi-lavoro/${spazioId}/report/mensile`, {
        params: { mese, anno },
        responseType: 'blob',
      })
      .pipe(
        catchError((errore: unknown) =>
          from(this.leggiErroreDaBlob(errore)).pipe(switchMap((leggibile) => throwError(() => leggibile))),
        ),
      );
  }

  /**
   * Con responseType "blob" anche il corpo di un errore arriva come Blob: lo si rilegge come JSON,
   * così il messaggio del backend resta leggibile (messaggioErrore funziona come per le altre chiamate).
   */
  private async leggiErroreDaBlob(errore: unknown): Promise<unknown> {
    if (errore instanceof HttpErrorResponse && errore.error instanceof Blob) {
      try {
        const testo = await errore.error.text();
        return new HttpErrorResponse({
          error: JSON.parse(testo),
          status: errore.status,
          statusText: errore.statusText,
          url: errore.url ?? undefined,
          headers: errore.headers,
        });
      } catch {
        return errore;
      }
    }
    return errore;
  }
}
