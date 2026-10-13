// src/app/core/services/autenticazione.service.ts
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AutenticazioneResponse,
  LoginRequest,
  RegistrazioneRequest,
} from '../../models/autenticazione.model';
import { Utente } from '../../models/utente.model';

/**
 * Gestisce la sessione dell'utente.
 * - access token: solo in memoria (sparisce se si ricarica la pagina)
 * - refresh token: in localStorage, serve a ottenere un nuovo access token
 */
@Injectable({ providedIn: 'root' })
export class AutenticazioneService {
  private static readonly CHIAVE_REFRESH = 'pf_refresh_token';

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly urlBase = `${environment.apiUrl}/auth`;

  private accessToken: string | null = null;
  /** Se un rinnovo è già in corso, le altre richieste lo condividono invece di farne un altro */
  private rinnovoInCorso$: Observable<AutenticazioneResponse> | null = null;

  readonly utente = signal<Utente | null>(null);
  readonly autenticato = computed(() => this.utente() !== null);
  readonly isAdmin = computed(() => this.utente()?.ruolo === 'ADMIN');

  login(richiesta: LoginRequest): Observable<Utente> {
    return this.http.post<AutenticazioneResponse>(`${this.urlBase}/login`, richiesta).pipe(
      tap((risposta) => this.salvaSessione(risposta)),
      map((risposta) => risposta.utente),
    );
  }

  registra(richiesta: RegistrazioneRequest): Observable<Utente> {
    return this.http.post<AutenticazioneResponse>(`${this.urlBase}/registrazione`, richiesta).pipe(
      tap((risposta) => this.salvaSessione(risposta)),
      map((risposta) => risposta.utente),
    );
  }

  /** Scambia il refresh token con una nuova coppia di token */
  rinnova(): Observable<AutenticazioneResponse> {
    if (this.rinnovoInCorso$) {
      return this.rinnovoInCorso$;
    }
    const refreshToken = localStorage.getItem(AutenticazioneService.CHIAVE_REFRESH);
    if (!refreshToken) {
      return throwError(() => new Error('Nessuna sessione da rinnovare'));
    }

    this.rinnovoInCorso$ = this.http
      .post<AutenticazioneResponse>(`${this.urlBase}/refresh`, { refreshToken })
      .pipe(
        tap((risposta) => this.salvaSessione(risposta)),
        catchError((errore: unknown) => {
          // Refresh token non più valido: la sessione è finita
          if (errore instanceof HttpErrorResponse && errore.status === 401) {
            this.cancellaSessione();
          }
          return throwError(() => errore);
        }),
        finalize(() => (this.rinnovoInCorso$ = null)),
        shareReplay(1),
      );
    return this.rinnovoInCorso$;
  }

  /** Chiude la sessione: avvisa il backend (senza aspettare la risposta) e torna al login */
  esci(): void {
    const refreshToken = localStorage.getItem(AutenticazioneService.CHIAVE_REFRESH);
    this.cancellaSessione();
    if (refreshToken) {
      this.http.post(`${this.urlBase}/logout`, { refreshToken }).subscribe({ error: () => undefined });
    }
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  haRefreshToken(): boolean {
    return localStorage.getItem(AutenticazioneService.CHIAVE_REFRESH) !== null;
  }

  /** Aggiorna i dati dell'utente mostrati nell'app (ad esempio dopo la modifica del profilo) */
  impostaUtente(utente: Utente): void {
    this.utente.set(utente);
  }

  private salvaSessione(risposta: AutenticazioneResponse): void {
    this.accessToken = risposta.accessToken;
    localStorage.setItem(AutenticazioneService.CHIAVE_REFRESH, risposta.refreshToken);
    this.utente.set(risposta.utente);
  }

  private cancellaSessione(): void {
    this.accessToken = null;
    localStorage.removeItem(AutenticazioneService.CHIAVE_REFRESH);
    this.utente.set(null);
  }
}
