// src/app/features/budget/budget.component.ts
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { BudgetService } from '../../core/services/budget.service';
import { CategoriaService } from '../../core/services/categoria.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { Budget, StatoBudget } from '../../models/budget.model';
import { Categoria } from '../../models/categoria.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { SelettoreMeseComponent } from '../../shared/componenti/selettore-mese/selettore-mese.component';
import { messaggioErrore } from '../../shared/utilita/errori';

@Component({
  selector: 'app-budget',
  imports: [ReactiveFormsModule, CurrencyPipe, DecimalPipe, SelettoreMeseComponent, ModaleConfermaComponent],
  template: `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <div>
        <h1 class="h3 mb-0">Budget</h1>
        @if (spaziLavoro.spazioAttivo(); as spazio) {
          <div class="text-secondary small">{{ spazio.nome }}</div>
        }
      </div>
      <app-selettore-mese [(mese)]="mese" [(anno)]="anno" [consentiFuturo]="true" />
    </div>

    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    @if (spaziLavoro.puoGestire()) {
      <div class="card border-0 shadow-sm mb-3">
        <div class="card-body">
          <h2 class="h6 mb-3">{{ inModifica() !== null ? 'Modifica budget' : 'Nuovo budget' }}</h2>
          <form [formGroup]="modulo" (ngSubmit)="salva()" novalidate>
            <div class="row g-2 align-items-end">
              <div class="col-12 col-md-5">
                <label class="form-label small mb-1" for="b-categoria">Categoria di spesa</label>
                <select id="b-categoria" class="form-select form-select-sm" formControlName="categoriaId"
                        [class.is-invalid]="nonValido('categoriaId')">
                  <option [ngValue]="null" disabled>Scegli una categoria</option>
                  @for (categoria of categorieSelezionabili(); track categoria.id) {
                    <option [ngValue]="categoria.id">{{ categoria.nome }}</option>
                  }
                </select>
                <div class="invalid-feedback">Scegli una categoria.</div>
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="b-importo">Importo mensile ({{ valuta() }})</label>
                <input id="b-importo" type="number" min="0.01" step="0.01" class="form-control form-control-sm"
                       formControlName="importo" [class.is-invalid]="nonValido('importo')" />
                <div class="invalid-feedback">Inserisci un importo maggiore di zero.</div>
              </div>
              <div class="col-6 col-md-4 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm" [disabled]="salvataggioInCorso()">
                  {{ inModifica() !== null ? 'Salva' : 'Aggiungi' }}
                </button>
                @if (inModifica() !== null) {
                  <button type="button" class="btn btn-light btn-sm" (click)="annullaModifica()">Annulla</button>
                }
              </div>
            </div>
            @if (inModifica() === null) {
              <div class="small text-secondary mt-2">
                Il budget vale per il mese selezionato in alto.
                @if (categorieSelezionabili().length === 0) {
                  Tutte le categorie di spesa hanno già un budget in questo mese.
                }
              </div>
            }
          </form>
        </div>
      </div>
    }

    <div class="card border-0 shadow-sm">
      @if (caricamento() && budget().length === 0) {
        <div class="card-body text-center text-secondary py-5">
          <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
        </div>
      } @else if (budget().length === 0) {
        <div class="card-body text-center text-secondary py-5">Nessun budget impostato per questo mese.</div>
      } @else {
        <div class="table-responsive" [class.opacity-50]="caricamento()">
          <table class="table align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>Categoria</th>
                <th class="text-end">Budget</th>
                <th class="text-end">Speso</th>
                <th class="text-end">Disponibile</th>
                <th class="colonna-utilizzo">Utilizzo</th>
                <th>Stato</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (voce of budget(); track voce.id) {
                <tr>
                  <td class="text-nowrap">
                    <span class="d-inline-block rounded-circle me-2 pallino"
                          [style.--colore]="voce.categoriaColore"></span>
                    {{ voce.categoriaNome }}
                  </td>
                  <td class="text-end text-nowrap">{{ voce.importo | currency: valuta() }}</td>
                  <td class="text-end text-nowrap">{{ voce.speso | currency: valuta() }}</td>
                  <td class="text-end text-nowrap" [class.text-danger]="voce.disponibile < 0">
                    {{ voce.disponibile | currency: valuta() }}
                  </td>
                  <td>
                    <div class="progress progress-medio">
                      <div class="progress-bar" role="progressbar" [class]="classeBarra(voce.stato)"
                           [style.--percentuale]="larghezza(voce)"></div>
                    </div>
                    <div class="small text-secondary mt-1">{{ voce.percentuale | number: '1.0-1' }}%</div>
                  </td>
                  <td>
                    <span class="badge" [class]="classeStato(voce.stato)">{{ etichettaStato(voce.stato) }}</span>
                  </td>
                  <td class="text-end text-nowrap">
                    @if (spaziLavoro.puoGestire()) {
                      <button type="button" class="btn btn-sm btn-outline-secondary me-1" title="Modifica importo"
                              (click)="modifica(voce)"><i class="bi bi-pencil"></i></button>
                      <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                              (click)="daEliminare.set(voce)"><i class="bi bi-trash"></i></button>
                    }
                  </td>
                </tr>
              }
            </tbody>
            <tfoot class="table-light">
              <tr class="fw-semibold">
                <td>Totale</td>
                <td class="text-end">{{ totaleBudget() | currency: valuta() }}</td>
                <td class="text-end">{{ totaleSpeso() | currency: valuta() }}</td>
                <td class="text-end" [class.text-danger]="totaleBudget() - totaleSpeso() < 0">
                  {{ totaleBudget() - totaleSpeso() | currency: valuta() }}
                </td>
                <td colspan="3"></td>
              </tr>
            </tfoot>
          </table>
        </div>
      }
    </div>

    @if (daEliminare(); as voce) {
      <app-modale-conferma
        titolo="Elimina budget"
        [messaggio]="messaggioEliminazione(voce)"
        etichettaConferma="Elimina"
        [inCorso]="eliminazioneInCorso()"
        (confermato)="eliminaConfermata(voce)"
        (annullato)="daEliminare.set(null)"
      />
    }
  `,
})
export class BudgetComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly budgetService = inject(BudgetService);
  private readonly categoriaService = inject(CategoriaService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  private readonly oggi = new Date();
  readonly mese = signal(this.oggi.getMonth() + 1);
  readonly anno = signal(this.oggi.getFullYear());

  readonly budget = signal<Budget[]>([]);
  readonly categorie = signal<Categoria[]>([]);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  readonly modulo = this.fb.group({
    categoriaId: this.fb.control<number | null>(null, [Validators.required]),
    importo: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
  });
  /** L'id del budget che si sta modificando; null se si sta creando */
  readonly inModifica = signal<number | null>(null);
  readonly salvataggioInCorso = signal(false);

  readonly daEliminare = signal<Budget | null>(null);
  readonly eliminazioneInCorso = signal(false);

  readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');
  readonly totaleBudget = computed(() => this.budget().reduce((somma, voce) => somma + voce.importo, 0));
  readonly totaleSpeso = computed(() => this.budget().reduce((somma, voce) => somma + voce.speso, 0));

  /** Solo le categorie di uscita senza budget nel mese (più quella del budget che si sta modificando) */
  readonly categorieSelezionabili = computed(() => {
    const giaUsate = new Set(this.budget().map((voce) => voce.categoriaId));
    const corrente = this.budget().find((voce) => voce.id === this.inModifica())?.categoriaId;
    return this.categorie().filter(
      (categoria) => categoria.tipo === 'USCITA' && (!giaUsate.has(categoria.id) || categoria.id === corrente),
    );
  });

  private richiestaInCorso?: Subscription;

  constructor() {
    // Le categorie dipendono dallo spazio attivo
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        this.categoriaService.elenca(spazioId).subscribe({
          next: (categorie) => this.categorie.set(categorie),
          error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
        });
      });
    });

    // I budget dipendono dallo spazio attivo e dal mese scelto
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      const mese = this.mese();
      const anno = this.anno();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        this.annullaModifica();
        this.carica(spazioId, mese, anno);
      });
    });
  }

  nonValido(campo: 'categoriaId' | 'importo'): boolean {
    const controllo = this.modulo.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  classeBarra(stato: StatoBudget): string {
    switch (stato) {
      case 'SUPERATO':
        return 'bg-danger';
      case 'ATTENZIONE':
        return 'bg-warning';
      default:
        return 'bg-success';
    }
  }

  classeStato(stato: StatoBudget): string {
    switch (stato) {
      case 'SUPERATO':
        return 'text-bg-danger';
      case 'ATTENZIONE':
        return 'text-bg-warning';
      default:
        return 'text-bg-success';
    }
  }

  etichettaStato(stato: StatoBudget): string {
    switch (stato) {
      case 'SUPERATO':
        return 'Superato';
      case 'ATTENZIONE':
        return 'Attenzione';
      default:
        return 'Nei limiti';
    }
  }

  /** La barra non va oltre il 100%, anche se il budget è stato superato */
  larghezza(voce: Budget): number {
    return Math.min(voce.percentuale, 100);
  }

  messaggioEliminazione(voce: Budget): string {
    return `Vuoi eliminare il budget "${voce.categoriaNome}" di questo mese? Le transazioni non vengono toccate.`;
  }

  modifica(voce: Budget): void {
    this.inModifica.set(voce.id);
    // Di un budget esistente si cambia solo l'importo: la categoria è la sua identità
    this.modulo.controls.categoriaId.disable();
    this.modulo.reset({ categoriaId: voce.categoriaId, importo: voce.importo });
  }

  annullaModifica(): void {
    this.inModifica.set(null);
    this.modulo.controls.categoriaId.enable();
    this.modulo.reset({ categoriaId: null, importo: null });
  }

  salva(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const v = this.modulo.getRawValue();
    const idModifica = this.inModifica();
    if (this.modulo.invalid || spazioId === null || v.importo === null || (idModifica === null && v.categoriaId === null)) {
      this.modulo.markAllAsTouched();
      return;
    }

    this.salvataggioInCorso.set(true);
    this.errore.set(null);
    const chiamata =
      idModifica === null
        ? this.budgetService.crea(spazioId, {
          categoriaId: v.categoriaId as number,
          importo: v.importo,
          mese: this.mese(),
          anno: this.anno(),
        })
        : this.budgetService.aggiorna(spazioId, idModifica, { importo: v.importo });

    chiamata.subscribe({
      next: () => {
        this.salvataggioInCorso.set(false);
        this.annullaModifica();
        this.carica(spazioId, this.mese(), this.anno());
      },
      error: (errore: unknown) => {
        // Ad esempio 409: esiste già un budget per questa categoria nel mese
        this.salvataggioInCorso.set(false);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  eliminaConfermata(voce: Budget): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.eliminazioneInCorso.set(true);
    this.budgetService.elimina(spazioId, voce.id).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.carica(spazioId, this.mese(), this.anno());
      },
      error: (errore: unknown) => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  private carica(spazioId: number, mese: number, anno: number): void {
    this.richiestaInCorso?.unsubscribe();
    this.caricamento.set(true);
    this.errore.set(null);

    this.richiestaInCorso = this.budgetService.elenca(spazioId, mese, anno).subscribe({
      next: (budget) => {
        this.budget.set(budget);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
