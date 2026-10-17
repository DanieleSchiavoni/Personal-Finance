// src/app/features/obiettivi/obiettivi.component.ts
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ObiettivoService } from '../../core/services/obiettivo.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { ObiettivoRequest, ObiettivoRisparmio, StatoObiettivo } from '../../models/obiettivo.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { dataDaIso } from '../../shared/utilita/date';
import { messaggioErrore } from '../../shared/utilita/errori';

@Component({
  selector: 'app-obiettivi',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, DecimalPipe, ModaleConfermaComponent],
  template: `
    <div class="mb-3">
      <h1 class="h3 mb-0">Obiettivi di risparmio</h1>
      @if (spaziLavoro.spazioAttivo(); as spazio) {
        <div class="text-secondary small">{{ spazio.nome }}</div>
      }
    </div>

    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    @if (spaziLavoro.puoGestire()) {
      <div class="card border-0 shadow-sm mb-4">
        <div class="card-body">
          <h2 class="h6 mb-3">{{ inModifica() !== null ? 'Modifica obiettivo' : 'Nuovo obiettivo' }}</h2>
          <form [formGroup]="modulo" (ngSubmit)="salva()" novalidate>
            <div class="row g-2 align-items-end">
              <div class="col-12 col-md-4">
                <label class="form-label small mb-1" for="o-nome">Nome</label>
                <input id="o-nome" type="text" class="form-control form-control-sm" formControlName="nome"
                       maxlength="100" placeholder="Ad esempio: Nuova automobile"
                       [class.is-invalid]="nonValido('nome')" />
                <div class="invalid-feedback">Il nome è obbligatorio.</div>
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="o-importo">Obiettivo ({{ valuta() }})</label>
                <input id="o-importo" type="number" min="0.01" step="0.01" class="form-control form-control-sm"
                       formControlName="importoObiettivo" [class.is-invalid]="nonValido('importoObiettivo')" />
                <div class="invalid-feedback">Inserisci un importo maggiore di zero.</div>
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="o-scadenza">Scadenza (facoltativa)</label>
                <input id="o-scadenza" type="date" class="form-control form-control-sm" formControlName="scadenza" />
              </div>
              <div class="col-12 col-md-8">
                <label class="form-label small mb-1" for="o-descrizione">Descrizione (facoltativa)</label>
                <input id="o-descrizione" type="text" class="form-control form-control-sm"
                       formControlName="descrizione" maxlength="255" />
              </div>
              <div class="col-12 col-md-4 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm" [disabled]="salvataggioInCorso()">
                  {{ inModifica() !== null ? 'Salva modifiche' : 'Aggiungi obiettivo' }}
                </button>
                @if (inModifica() !== null) {
                  <button type="button" class="btn btn-light btn-sm" (click)="annullaModifica()">Annulla</button>
                }
              </div>
            </div>
          </form>
        </div>
      </div>
    }

    @if (caricamento() && obiettivi().length === 0) {
      <div class="text-center text-secondary py-5">
        <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
      </div>
    } @else if (obiettivi().length === 0) {
      <div class="card border-0 shadow-sm">
        <div class="card-body text-center text-secondary py-5">
          <i class="bi bi-bullseye fs-3 d-block mb-2"></i>Non ci sono ancora obiettivi di risparmio.
        </div>
      </div>
    } @else {
      <div class="row g-3">
        @for (obiettivo of obiettivi(); track obiettivo.id) {
          <div class="col-12 col-md-6 col-xl-4">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body d-flex flex-column">
                <div class="d-flex justify-content-between align-items-start gap-2 mb-1">
                  <h2 class="h6 mb-0">{{ obiettivo.nome }}</h2>
                  <span class="badge" [class]="classeStato(obiettivo.stato)">{{ etichettaStato(obiettivo.stato) }}</span>
                </div>
                @if (obiettivo.descrizione) {
                  <p class="small text-secondary mb-2">{{ obiettivo.descrizione }}</p>
                }

                <div class="d-flex justify-content-between small mt-2 mb-1">
                  <span class="fw-semibold">{{ obiettivo.importoAttuale | currency: valuta() }}</span>
                  <span class="text-secondary">{{ obiettivo.importoObiettivo | currency: valuta() }}</span>
                </div>
                <div class="progress" style="height: 10px">
                  <div class="progress-bar" role="progressbar" [class]="classeBarra(obiettivo.stato)"
                       [style.width.%]="obiettivo.percentuale"></div>
                </div>
                <div class="d-flex justify-content-between small text-secondary mt-1">
                  <span>{{ obiettivo.percentuale | number: '1.0-1' }}%</span>
                  @if (obiettivo.importoMancante > 0) {
                    <span>Mancano {{ obiettivo.importoMancante | currency: valuta() }}</span>
                  }
                </div>

                @if (obiettivo.scadenza) {
                  <div class="small text-secondary mt-2">
                    <i class="bi bi-calendar-event me-1"></i>Scadenza {{ obiettivo.scadenza | date: 'dd/MM/yyyy' }}
                    @if (giorniRimanenti(obiettivo); as giorni) {
                      · {{ giorni }} {{ giorni === 1 ? 'giorno' : 'giorni' }}
                    }
                  </div>
                }

                @if (spaziLavoro.puoGestire()) {
                  <div class="d-flex flex-wrap gap-2 mt-3 pt-3 border-top mt-auto">
                    <button type="button" class="btn btn-sm btn-primary" (click)="apriVersamento(obiettivo)">
                      <i class="bi bi-plus-lg me-1"></i>Aggiungi denaro
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-secondary ms-auto" title="Modifica"
                            (click)="modifica(obiettivo)"><i class="bi bi-pencil"></i></button>
                    <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                            (click)="daEliminare.set(obiettivo)"><i class="bi bi-trash"></i></button>
                  </div>
                }
              </div>
            </div>
          </div>
        }
      </div>
    }

    @if (daVersare(); as obiettivo) {
      <app-modale-conferma
        [titolo]="'Aggiungi denaro a ' + obiettivo.nome"
        [messaggio]="'Inserisci l\\'importo da aggiungere (' + valuta() + ').'"
        etichettaConferma="Aggiungi"
        stile="primary"
        [inCorso]="versamentoInCorso()"
        [confermaDisabilitata]="importoVersamento.invalid"
        (confermato)="versa(obiettivo)"
        (annullato)="daVersare.set(null)"
      >
        <input type="number" min="0.01" step="0.01" class="form-control mt-3" placeholder="Importo"
               [formControl]="importoVersamento" (keydown.enter)="versa(obiettivo)" />
        @if (erroreVersamento()) {
          <div class="text-danger small mt-2">{{ erroreVersamento() }}</div>
        }
      </app-modale-conferma>
    }

    @if (daEliminare(); as obiettivo) {
      <app-modale-conferma
        titolo="Elimina obiettivo"
        [messaggio]="'Vuoi eliminare l\\'obiettivo &quot;' + obiettivo.nome + '&quot;? Il risparmio registrato andrà perso.'"
        etichettaConferma="Elimina"
        [inCorso]="eliminazioneInCorso()"
        (confermato)="eliminaConfermata(obiettivo)"
        (annullato)="daEliminare.set(null)"
      />
    }
  `,
})
export class ObiettiviComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly obiettivoService = inject(ObiettivoService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly obiettivi = signal<ObiettivoRisparmio[]>([]);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  readonly modulo = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    descrizione: ['', [Validators.maxLength(255)]],
    importoObiettivo: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    scadenza: [''],
  });
  readonly inModifica = signal<number | null>(null);
  readonly salvataggioInCorso = signal(false);

  readonly daVersare = signal<ObiettivoRisparmio | null>(null);
  readonly importoVersamento = new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]);
  readonly versamentoInCorso = signal(false);
  readonly erroreVersamento = signal<string | null>(null);

  readonly daEliminare = signal<ObiettivoRisparmio | null>(null);
  readonly eliminazioneInCorso = signal(false);

  readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');

  constructor() {
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        this.annullaModifica();
        this.carica(spazioId);
      });
    });
  }

  nonValido(campo: 'nome' | 'importoObiettivo'): boolean {
    const controllo = this.modulo.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  classeStato(stato: StatoObiettivo): string {
    switch (stato) {
      case 'COMPLETATO':
        return 'text-bg-success';
      case 'SCADUTO':
        return 'text-bg-danger';
      default:
        return 'text-bg-primary';
    }
  }

  classeBarra(stato: StatoObiettivo): string {
    switch (stato) {
      case 'COMPLETATO':
        return 'bg-success';
      case 'SCADUTO':
        return 'bg-danger';
      default:
        return 'bg-primary';
    }
  }

  etichettaStato(stato: StatoObiettivo): string {
    switch (stato) {
      case 'COMPLETATO':
        return 'Raggiunto';
      case 'SCADUTO':
        return 'Scaduto';
      default:
        return 'In corso';
    }
  }

  /** I giorni che mancano alla scadenza, solo per gli obiettivi ancora in corso */
  giorniRimanenti(obiettivo: ObiettivoRisparmio): number | null {
    if (!obiettivo.scadenza || obiettivo.stato !== 'IN_CORSO') {
      return null;
    }
    const oggi = new Date();
    oggi.setHours(0, 0, 0, 0);
    const differenza = dataDaIso(obiettivo.scadenza).getTime() - oggi.getTime();
    return Math.round(differenza / 86_400_000);
  }

  modifica(obiettivo: ObiettivoRisparmio): void {
    this.inModifica.set(obiettivo.id);
    this.modulo.reset({
      nome: obiettivo.nome,
      descrizione: obiettivo.descrizione ?? '',
      importoObiettivo: obiettivo.importoObiettivo,
      scadenza: obiettivo.scadenza ?? '',
    });
  }

  annullaModifica(): void {
    this.inModifica.set(null);
    this.modulo.reset({ nome: '', descrizione: '', importoObiettivo: null, scadenza: '' });
  }

  salva(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const v = this.modulo.getRawValue();
    if (this.modulo.invalid || spazioId === null || v.importoObiettivo === null) {
      this.modulo.markAllAsTouched();
      return;
    }
    const richiesta: ObiettivoRequest = {
      nome: v.nome.trim(),
      descrizione: v.descrizione.trim() || null,
      importoObiettivo: v.importoObiettivo,
      scadenza: v.scadenza || null,
    };

    this.salvataggioInCorso.set(true);
    this.errore.set(null);
    const idModifica = this.inModifica();
    const chiamata =
      idModifica === null
        ? this.obiettivoService.crea(spazioId, richiesta)
        : this.obiettivoService.aggiorna(spazioId, idModifica, richiesta);

    chiamata.subscribe({
      next: () => {
        this.salvataggioInCorso.set(false);
        this.annullaModifica();
        this.carica(spazioId);
      },
      error: (errore: unknown) => {
        // Ad esempio 400: la scadenza non può essere una data passata
        this.salvataggioInCorso.set(false);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  apriVersamento(obiettivo: ObiettivoRisparmio): void {
    this.importoVersamento.reset(null);
    this.erroreVersamento.set(null);
    this.daVersare.set(obiettivo);
  }

  versa(obiettivo: ObiettivoRisparmio): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const importo = this.importoVersamento.value;
    if (this.importoVersamento.invalid || spazioId === null || importo === null || this.versamentoInCorso()) {
      return;
    }
    this.versamentoInCorso.set(true);
    this.erroreVersamento.set(null);

    this.obiettivoService.versa(spazioId, obiettivo.id, importo).subscribe({
      next: (aggiornato) => {
        this.obiettivi.update((lista) => lista.map((voce) => (voce.id === aggiornato.id ? aggiornato : voce)));
        this.versamentoInCorso.set(false);
        this.daVersare.set(null);
      },
      error: (errore: unknown) => {
        this.erroreVersamento.set(messaggioErrore(errore));
        this.versamentoInCorso.set(false);
      },
    });
  }

  eliminaConfermata(obiettivo: ObiettivoRisparmio): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.eliminazioneInCorso.set(true);
    this.obiettivoService.elimina(spazioId, obiettivo.id).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.carica(spazioId);
      },
      error: (errore: unknown) => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  private carica(spazioId: number): void {
    this.caricamento.set(true);
    this.errore.set(null);
    this.obiettivoService.elenca(spazioId).subscribe({
      next: (obiettivi) => {
        this.obiettivi.set(obiettivi);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
