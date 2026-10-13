// src/app/core/services/spazio-lavoro.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CreaSpazioLavoroRequest, SpazioLavoro } from '../../models/spazio-lavoro.model';

/** Gli spazi dell'utente e quello attualmente selezionato: tutte le pagine lavorano sullo spazio attivo */
@Injectable({ providedIn: 'root' })
export class SpazioLavoroService {
  private static readonly CHIAVE_SPAZIO_ATTIVO = 'pf_spazio_attivo';

  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/spazi-lavoro`;

  readonly elenco = signal<SpazioLavoro[]>([]);
  readonly spazioAttivoId = signal<number | null>(null);

  readonly spazioAttivo = computed(
    () => this.elenco().find((spazio) => spazio.id === this.spazioAttivoId()) ?? null,
  );
  /** PROPRIETARIO o AMMINISTRATORE: possono gestire membri, categorie, budget, dipendenti */
  readonly puoGestire = computed(() => {
    const ruolo = this.spazioAttivo()?.ruoloUtente;
    return ruolo === 'PROPRIETARIO' || ruolo === 'AMMINISTRATORE';
  });
  /** Chiunque non sia LETTORE può inserire dati */
  readonly puoScrivere = computed(() => {
    const ruolo = this.spazioAttivo()?.ruoloUtente;
    return ruolo !== undefined && ruolo !== 'LETTORE';
  });

  /** Carica gli spazi e seleziona l'ultimo usato (o, in mancanza, il personale) */
  carica(): Observable<SpazioLavoro[]> {
    return this.http.get<SpazioLavoro[]>(this.url).pipe(
      tap((spazi) => {
        this.elenco.set(spazi);
        const salvato = Number(localStorage.getItem(SpazioLavoroService.CHIAVE_SPAZIO_ATTIVO));
        // Il backend restituisce per primo lo spazio personale
        const scelto = spazi.find((spazio) => spazio.id === salvato) ?? spazi[0] ?? null;
        this.spazioAttivoId.set(scelto ? scelto.id : null);
      }),
    );
  }

  seleziona(id: number): void {
    this.spazioAttivoId.set(id);
    localStorage.setItem(SpazioLavoroService.CHIAVE_SPAZIO_ATTIVO, String(id));
  }

  creaAziendale(nome: string): Observable<SpazioLavoro> {
    const richiesta: CreaSpazioLavoroRequest = { nome };
    return this.http.post<SpazioLavoro>(this.url, richiesta).pipe(
      tap((creato) => {
        this.elenco.update((spazi) => [...spazi, creato]);
        this.seleziona(creato.id);
      }),
    );
  }

  /** Da chiamare quando l'utente esce, per non mostrare spazi di un altro account */
  reset(): void {
    this.elenco.set([]);
    this.spazioAttivoId.set(null);
  }
}
