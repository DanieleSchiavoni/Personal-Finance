// src/app/features/admin/admin-utenti.component.ts
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { AdminService } from '../../core/services/admin.service';
import { AutenticazioneService } from '../../core/services/autenticazione.service';
import { Pagina } from '../../models/pagina.model';
import { Utente } from '../../models/utente.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { messaggioErrore } from '../../shared/utilita/errori';

const DIMENSIONE_PAGINA = 10;

@Component({
  selector: 'app-admin-utenti',
  imports: [ReactiveFormsModule, DatePipe, ModaleConfermaComponent],
  template: `
    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    <div class="input-group input-group-sm mb-3 larghezza-max-420">
      <input type="search" class="form-control" placeholder="Cerca per nome, username o email"
             [formControl]="testo" (keydown.enter)="cerca()" />
      <button type="button" class="btn btn-primary" (click)="cerca()"><i class="bi bi-search"></i></button>
    </div>

    <div class="card border-0 shadow-sm">
      @if (caricamento() && !risultato()) {
        <div class="card-body text-secondary">
          <span class="spinner-border spinner-border-sm me-2"></span>Caricamento…
        </div>
      } @else {
        @if (risultato(); as dati) {
          @if (dati.contenuto.length === 0) {
            <div class="card-body text-center text-secondary py-4">Nessun utente trovato.</div>
          } @else {
            <div class="table-responsive">
              <table class="table align-middle mb-0">
                <thead class="table-light">
                <tr>
                  <th>Utente</th>
                  <th>Email</th>
                  <th>Ruolo</th>
                  <th>Stato</th>
                  <th>Iscritto dal</th>
                  <th></th>
                </tr>
                </thead>
                <tbody>
                  @for (utente of dati.contenuto; track utente.id) {
                    <tr>
                      <td>
                        {{ utente.nome }} {{ utente.cognome }}
                        <div class="small text-secondary">&#64;{{ utente.username }}</div>
                      </td>
                      <td>{{ utente.email }}</td>
                      <td>
                    <span class="badge"
                          [class.text-bg-primary]="utente.ruolo === 'ADMIN'"
                          [class.text-bg-secondary]="utente.ruolo === 'UTENTE'">
                      {{ utente.ruolo === 'ADMIN' ? 'Admin' : 'Utente' }}
                    </span>
                      </td>
                      <td>
                    <span class="badge"
                          [class.text-bg-success]="utente.abilitato"
                          [class.text-bg-danger]="!utente.abilitato">
                      {{ utente.abilitato ? 'Attivo' : 'Bloccato' }}
                    </span>
                      </td>
                      <td class="text-nowrap">{{ utente.dataCreazione | date: 'dd/MM/yyyy' }}</td>
                      <td class="text-end">
                        @if (utente.id !== auth.utente()?.id) {
                          @if (utente.abilitato) {
                            <button type="button" class="btn btn-sm btn-outline-danger" (click)="daBloccare.set(utente)">
                              Blocca
                            </button>
                          } @else {
                            <button type="button" class="btn btn-sm btn-outline-success" [disabled]="operazioneInCorso()"
                                    (click)="imposta(utente, true)">
                              Sblocca
                            </button>
                          }
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <div class="card-footer bg-white d-flex flex-wrap justify-content-between align-items-center gap-2">
          <span class="small text-secondary">
            Pagina {{ dati.pagina + 1 }} di {{ dati.totalePagine }} · {{ dati.totaleElementi }} utenti
          </span>
              <div class="btn-group">
                <button type="button" class="btn btn-outline-secondary btn-sm" [disabled]="dati.pagina === 0"
                        (click)="vaiAPagina(dati.pagina - 1)"><i class="bi bi-chevron-left"></i> Precedente</button>
                <button type="button" class="btn btn-outline-secondary btn-sm"
                        [disabled]="dati.pagina + 1 >= dati.totalePagine"
                        (click)="vaiAPagina(dati.pagina + 1)">Successiva <i class="bi bi-chevron-right"></i></button>
              </div>
            </div>
          }
        }
      }
    </div>

    @if (daBloccare(); as utente) {
      <app-modale-conferma
        titolo="Blocca utente"
        [messaggio]="'Vuoi bloccare ' + utente.nome + ' ' + utente.cognome + '? Perderà subito l\\'accesso e le sue sessioni verranno chiuse.'"
        etichettaConferma="Blocca"
        [inCorso]="operazioneInCorso()"
        (confermato)="imposta(utente, false)"
        (annullato)="daBloccare.set(null)"
      />
    }
  `,
})
export class AdminUtentiComponent implements OnInit {
  private readonly admin = inject(AdminService);
  readonly auth = inject(AutenticazioneService);

  readonly testo = new FormControl('', { nonNullable: true });
  readonly risultato = signal<Pagina<Utente> | null>(null);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);
  readonly daBloccare = signal<Utente | null>(null);
  readonly operazioneInCorso = signal(false);

  private paginaCorrente = 0;

  ngOnInit(): void {
    this.carica();
  }

  cerca(): void {
    this.paginaCorrente = 0;
    this.carica();
  }

  vaiAPagina(pagina: number): void {
    this.paginaCorrente = pagina;
    this.carica();
  }

  imposta(utente: Utente, abilitato: boolean): void {
    this.operazioneInCorso.set(true);
    this.errore.set(null);
    this.admin.impostaAbilitazione(utente.id, abilitato).subscribe({
      next: (aggiornato) => {
        this.risultato.update((pagina) =>
          pagina
            ? { ...pagina, contenuto: pagina.contenuto.map((u) => (u.id === aggiornato.id ? aggiornato : u)) }
            : pagina,
        );
        this.operazioneInCorso.set(false);
        this.daBloccare.set(null);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.operazioneInCorso.set(false);
        this.daBloccare.set(null);
      },
    });
  }

  private carica(): void {
    this.caricamento.set(true);
    this.errore.set(null);
    this.admin
      .elencaUtenti({
        testo: this.testo.value.trim() || undefined,
        pagina: this.paginaCorrente,
        dimensione: DIMENSIONE_PAGINA,
      })
      .subscribe({
        next: (pagina) => {
          this.risultato.set(pagina);
          this.caricamento.set(false);
        },
        error: (errore: unknown) => {
          this.errore.set(messaggioErrore(errore));
          this.caricamento.set(false);
        },
      });
  }
}
