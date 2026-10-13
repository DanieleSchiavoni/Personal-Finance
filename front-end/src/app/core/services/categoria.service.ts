// src/app/core/services/categoria.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Categoria, CategoriaRequest } from '../../models/categoria.model';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private readonly http = inject(HttpClient);

  private url(spazioId: number): string {
    return `${environment.apiUrl}/spazi-lavoro/${spazioId}/categorie`;
  }

  /** Le categorie dello spazio più quelle predefinite adatte al suo tipo */
  elenca(spazioId: number): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.url(spazioId));
  }

  crea(spazioId: number, richiesta: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.url(spazioId), richiesta);
  }

  aggiorna(spazioId: number, id: number, richiesta: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.url(spazioId)}/${id}`, richiesta);
  }

  elimina(spazioId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.url(spazioId)}/${id}`);
  }
}
