// src/app/features/team/team.component.ts
import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AutenticazioneService } from '../../core/services/autenticazione.service';
import { MembroService } from '../../core/services/membro.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { ETICHETTE_RUOLO, Membro } from '../../models/membro.model';
import { RuoloSpazio } from '../../models/spazio-lavoro.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { messaggioErrore } from '../../shared/utilita/errori';

@Component({
  selector: 'app-team',
  imports: [ReactiveFormsModule, DatePipe, ModaleConfermaComponent],
  template: `
    <h1 class="h3 mb-3">Team</h1>

    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    @if (!eAziendale()) {
      <div class="alert alert-info">
        Lo spazio personale ha un solo membro: tu. Per lavorare con altre persone crea uno spazio aziendale
        dal selettore nella barra laterale.
      </div>
    } @else {
      <div class="card border-0 shadow-sm mb-4">
        <div class="card-header bg-white fw-semibold">Membri ({{ membri().length }})</div>
        @if (caricamento()) {
          <div class="card-body text-secondary">
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
          </div>
        } @else {
          <div class="table-responsive">
            <table class="table align-middle mb-0">
              <thead class="table-light">
                <tr><th>Nome</th><th>Email</th><th>Ruolo</th><th>Dal</th><th></th></tr>
              </thead>
              <tbody>
                @for (membro of membri(); track membro.utenteId) {
                  <tr>
                    <td>
                      {{ membro.nome }} {{ membro.cognome }}
                      @if (eIo(membro)) {
                        <span class="badge text-bg-light border ms-1">tu</span>
                      }
                      <div class="small text-secondary">&#64;{{ membro.username }}</div>
                    </td>
                    <td>{{ membro.email }}</td>
                    <td>
                      @if (puoCambiareRuolo(membro)) {
                        <select class="form-select form-select-sm" style="width: auto"
                                (change)="cambiaRuolo(membro, $event)">
                          @for (ruolo of ruoliAssegnabili(); track ruolo) {
                            <option [value]="ruolo" [selected]="ruolo === membro.ruolo">{{ etichette[ruolo] }}</option>
                          }
                        </select>
                      } @else {
                        <span class="badge" [class]="classeRuolo(membro.ruolo)">{{ etichette[membro.ruolo] }}</span>
                      }
                    </td>
                    <td class="text-nowrap">{{ membro.dataIngresso | date: 'dd/MM/yyyy' }}</td>
                    <td class="text-end">
                      @if (puoRimuovere(membro)) {
                        <button type="button" class="btn btn-sm btn-outline-danger"
                                [title]="eIo(membro) ? 'Esci dallo spazio' : 'Rimuovi dallo spazio'"
                                (click)="daRimuovere.set(membro)">
                          <i class="bi" [class.bi-box-arrow-right]="eIo(membro)"
                             [class.bi-person-dash]="!eIo(membro)"></i>
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

      @if (spaziLavoro.puoGestire()) {
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h2 class="h6 mb-3">Aggiungi un membro</h2>
            <p class="small text-secondary">La persona deve essere già registrata: indica la sua email.</p>
            @if (messaggioAggiunta()) {
              <div class="alert alert-success py-2">{{ messaggioAggiunta() }}</div>
            }
            @if (erroreAggiunta()) {
              <div class="alert alert-danger py-2">{{ erroreAggiunta() }}</div>
            }
            <form [formGroup]="nuovoMembro" (ngSubmit)="aggiungi()" novalidate>
              <div class="row g-2 align-items-end">
                <div class="col-12 col-md-6">
                  <label class="form-label small mb-1" for="m-email">Email</label>
                  <input id="m-email" type="email" class="form-control form-control-sm" formControlName="email"
                         placeholder="nome@azienda.it" [class.is-invalid]="emailNonValida()" />
                  <div class="invalid-feedback">Inserisci un indirizzo email valido.</div>
                </div>
                <div class="col-6 col-md-3">
                  <label class="form-label small mb-1" for="m-ruolo">Ruolo</label>
                  <select id="m-ruolo" class="form-select form-select-sm" formControlName="ruolo">
                    @for (ruolo of ruoliAssegnabili(); track ruolo) {
                      <option [value]="ruolo">{{ etichette[ruolo] }}</option>
                    }
                  </select>
                </div>
                <div class="col-6 col-md-3">
                  <button type="submit" class="btn btn-primary btn-sm w-100" [disabled]="aggiuntaInCorso()">
                    <i class="bi bi-person-plus me-1"></i>Aggiungi
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>

        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body">
            <h2 class="h6 mb-3">Impostazioni dello spazio</h2>
            @if (messaggioImpostazioni()) {
              <div class="alert alert-success py-2">{{ messaggioImpostazioni() }}</div>
            }
            @if (erroreImpostazioni()) {
              <div class="alert alert-danger py-2">{{ erroreImpostazioni() }}</div>
            }
            <form [formGroup]="impostazioni" (ngSubmit)="salvaImpostazioni()" novalidate>
              <div class="row g-2 align-items-end">
                <div class="col-12 col-md-6">
                  <label class="form-label small mb-1" for="i-nome">Nome</label>
                  <input id="i-nome" type="text" class="form-control form-control-sm" formControlName="nome"
                         maxlength="100" [class.is-invalid]="impostazioniNonValide('nome')" />
                  <div class="invalid-feedback">Il nome è obbligatorio.</div>
                </div>
                <div class="col-6 col-md-3">
                  <label class="form-label small mb-1" for="i-valuta">Valuta</label>
                  <input id="i-valuta" type="text" class="form-control form-control-sm text-uppercase"
                         formControlName="valuta" maxlength="3" [class.is-invalid]="impostazioniNonValide('valuta')" />
                  <div class="invalid-feedback">Codice di 3 lettere (es. EUR).</div>
                </div>
                <div class="col-6 col-md-3">
                  <button type="submit" class="btn btn-primary btn-sm w-100" [disabled]="salvataggioImpostazioni()">
                    Salva
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      }

      @if (eProprietario()) {
        <div class="card border border-danger-subtle shadow-sm">
          <div class="card-body d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <h2 class="h6 text-danger mb-1">Elimina lo spazio</h2>
              <p class="small text-secondary mb-0">
                Elimina lo spazio con tutti i suoi dati: transazioni, budget, categorie, dipendenti e obiettivi.
                L'operazione non si può annullare.
              </p>
            </div>
            <button type="button" class="btn btn-outline-danger" (click)="eliminazioneSpazioVisibile.set(true)">
              Elimina spazio
            </button>
          </div>
        </div>
      }
    }

    @if (daRimuovere(); as membro) {
      <app-modale-conferma
        [titolo]="eIo(membro) ? 'Esci dallo spazio' : 'Rimuovi membro'"
        [messaggio]="messaggioRimozione(membro)"
        [etichettaConferma]="eIo(membro) ? 'Esci' : 'Rimuovi'"
        [inCorso]="rimozioneInCorso()"
        (confermato)="rimuoviConfermato(membro)"
        (annullato)="daRimuovere.set(null)"
      />
    }

    @if (eliminazioneSpazioVisibile()) {
      <app-modale-conferma
        titolo="Elimina spazio"
        [messaggio]="messaggioEliminazioneSpazio()"
        etichettaConferma="Elimina definitivamente"
        [inCorso]="eliminazioneSpazioInCorso()"
        (confermato)="eliminaSpazio()"
        (annullato)="eliminazioneSpazioVisibile.set(false)"
      />
    }
  `,
})
export class TeamComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);
  private readonly membroService = inject(MembroService);
  readonly auth = inject(AutenticazioneService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly etichette = ETICHETTE_RUOLO;

  readonly membri = signal<Membro[]>([]);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  readonly nuovoMembro = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    ruolo: this.fb.control<RuoloSpazio>('MEMBRO'),
  });
  readonly aggiuntaInCorso = signal(false);
  readonly messaggioAggiunta = signal<string | null>(null);
  readonly erroreAggiunta = signal<string | null>(null);

  readonly impostazioni = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    valuta: ['EUR', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
  });
  readonly salvataggioImpostazioni = signal(false);
  readonly messaggioImpostazioni = signal<string | null>(null);
  readonly erroreImpostazioni = signal<string | null>(null);

  readonly daRimuovere = signal<Membro | null>(null);
  readonly rimozioneInCorso = signal(false);
  readonly eliminazioneSpazioVisibile = signal(false);
  readonly eliminazioneSpazioInCorso = signal(false);

  readonly eAziendale = computed(() => this.spaziLavoro.spazioAttivo()?.tipo === 'AZIENDALE');
  readonly eProprietario = computed(() => this.spaziLavoro.spazioAttivo()?.ruoloUtente === 'PROPRIETARIO');
  /** Solo il proprietario può nominare amministratori */
  readonly ruoliAssegnabili = computed<RuoloSpazio[]>(() =>
    this.eProprietario() ? ['AMMINISTRATORE', 'MEMBRO', 'LETTORE'] : ['MEMBRO', 'LETTORE'],
  );

  constructor() {
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        const spazio = this.spaziLavoro.spazioAttivo();
        if (spazio) {
          this.impostazioni.reset({ nome: spazio.nome, valuta: spazio.valuta });
        }
        this.membri.set([]);
        this.errore.set(null);
        this.messaggioAggiunta.set(null);
        this.messaggioImpostazioni.set(null);
        if (spazio?.tipo === 'AZIENDALE') {
          this.caricaMembri(spazioId);
        }
      });
    });
  }

  eIo(membro: Membro): boolean {
    return membro.utenteId === this.auth.utente()?.id;
  }

  puoCambiareRuolo(membro: Membro): boolean {
    return this.eProprietario() && membro.ruolo !== 'PROPRIETARIO';
  }

  puoRimuovere(membro: Membro): boolean {
    if (membro.ruolo === 'PROPRIETARIO') {
      return false;
    }
    if (this.eIo(membro)) {
      return true; // ognuno può uscire dallo spazio
    }
    const mioRuolo = this.spaziLavoro.spazioAttivo()?.ruoloUtente;
    if (mioRuolo === 'PROPRIETARIO') {
      return true;
    }
    return mioRuolo === 'AMMINISTRATORE' && membro.ruolo !== 'AMMINISTRATORE';
  }

  classeRuolo(ruolo: RuoloSpazio): string {
    switch (ruolo) {
      case 'PROPRIETARIO':
        return 'text-bg-primary';
      case 'AMMINISTRATORE':
        return 'text-bg-info';
      case 'MEMBRO':
        return 'text-bg-secondary';
      default:
        return 'text-bg-light border';
    }
  }

  emailNonValida(): boolean {
    const email = this.nuovoMembro.controls.email;
    return email.invalid && (email.touched || email.dirty);
  }

  impostazioniNonValide(campo: 'nome' | 'valuta'): boolean {
    const controllo = this.impostazioni.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  messaggioRimozione(membro: Membro): string {
    return this.eIo(membro)
      ? 'Vuoi uscire da questo spazio? Per rientrare dovrà aggiungerti di nuovo chi lo gestisce.'
      : `Vuoi rimuovere ${membro.nome} ${membro.cognome} da questo spazio?`;
  }

  messaggioEliminazioneSpazio(): string {
    const nome = this.spaziLavoro.spazioAttivo()?.nome ?? '';
    return `Vuoi eliminare definitivamente lo spazio "${nome}" con tutti i suoi dati?`;
  }

  aggiungi(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (this.nuovoMembro.invalid || spazioId === null) {
      this.nuovoMembro.markAllAsTouched();
      return;
    }
    this.aggiuntaInCorso.set(true);
    this.messaggioAggiunta.set(null);
    this.erroreAggiunta.set(null);

    const v = this.nuovoMembro.getRawValue();
    this.membroService.aggiungi(spazioId, { email: v.email.trim(), ruolo: v.ruolo }).subscribe({
      next: (nuovo) => {
        this.membri.update((lista) => [...lista, nuovo]);
        this.nuovoMembro.reset();
        this.messaggioAggiunta.set(`${nuovo.nome} ${nuovo.cognome} è stato aggiunto allo spazio.`);
        this.aggiuntaInCorso.set(false);
      },
      error: (errore: unknown) => {
        // Ad esempio: nessun utente con questa email (404), già nello spazio (409)
        this.erroreAggiunta.set(messaggioErrore(errore));
        this.aggiuntaInCorso.set(false);
      },
    });
  }

  cambiaRuolo(membro: Membro, evento: Event): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const ruolo = (evento.target as HTMLSelectElement).value as RuoloSpazio;
    if (spazioId === null || ruolo === membro.ruolo) {
      return;
    }
    this.errore.set(null);
    this.membroService.cambiaRuolo(spazioId, membro.utenteId, ruolo).subscribe({
      next: (aggiornato) =>
        this.membri.update((lista) => lista.map((m) => (m.utenteId === aggiornato.utenteId ? aggiornato : m))),
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricaMembri(spazioId); // si ricarica per riportare il menu al valore reale
      },
    });
  }

  rimuoviConfermato(membro: Membro): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.rimozioneInCorso.set(true);
    this.membroService.rimuovi(spazioId, membro.utenteId).subscribe({
      next: () => {
        this.rimozioneInCorso.set(false);
        this.daRimuovere.set(null);
        if (this.eIo(membro)) {
          // Si è usciti dallo spazio: si ricarica l'elenco degli spazi e si torna alla dashboard
          this.spaziLavoro.carica().subscribe({
            next: () => this.router.navigate(['/dashboard']),
            error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
          });
        } else {
          this.membri.update((lista) => lista.filter((m) => m.utenteId !== membro.utenteId));
        }
      },
      error: (errore: unknown) => {
        this.rimozioneInCorso.set(false);
        this.daRimuovere.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  salvaImpostazioni(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (this.impostazioni.invalid || spazioId === null) {
      this.impostazioni.markAllAsTouched();
      return;
    }
    this.salvataggioImpostazioni.set(true);
    this.messaggioImpostazioni.set(null);
    this.erroreImpostazioni.set(null);

    const v = this.impostazioni.getRawValue();
    this.spaziLavoro.aggiorna(spazioId, { nome: v.nome.trim(), valuta: v.valuta.toUpperCase() }).subscribe({
      next: () => {
        this.messaggioImpostazioni.set('Impostazioni salvate.');
        this.salvataggioImpostazioni.set(false);
      },
      error: (errore: unknown) => {
        this.erroreImpostazioni.set(messaggioErrore(errore));
        this.salvataggioImpostazioni.set(false);
      },
    });
  }

  eliminaSpazio(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.eliminazioneSpazioInCorso.set(true);
    this.spaziLavoro.elimina(spazioId).subscribe({
      next: () => {
        this.eliminazioneSpazioInCorso.set(false);
        this.eliminazioneSpazioVisibile.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (errore: unknown) => {
        this.eliminazioneSpazioInCorso.set(false);
        this.eliminazioneSpazioVisibile.set(false);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  private caricaMembri(spazioId: number): void {
    this.caricamento.set(true);
    this.membroService.elenca(spazioId).subscribe({
      next: (membri) => {
        this.membri.set(membri);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
