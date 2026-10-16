// src/app/features/dashboard/dashboard.component.ts
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { DashboardService } from '../../core/services/dashboard.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { RiepilogoDashboard } from '../../models/dashboard.model';
import { SchedaKpiComponent } from '../../shared/componenti/scheda-kpi/scheda-kpi.component';
import { messaggioErrore } from '../../shared/utilita/errori';
import { GraficiAndamentoComponent } from './grafici/grafici-andamento.component';
import { GraficoBudgetComponent } from './grafici/grafico-budget.component';
import { GraficoSpeseCategoriaComponent } from './grafici/grafico-spese-categoria.component';

@Component({
  selector: 'app-dashboard',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    RouterLink,
    SchedaKpiComponent,
    GraficiAndamentoComponent,
    GraficoSpeseCategoriaComponent,
    GraficoBudgetComponent,
  ],
  template: `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <div>
        <h1 class="h3 mb-0">Dashboard</h1>
        @if (spaziLavoro.spazioAttivo(); as spazio) {
          <div class="text-secondary small">{{ spazio.nome }}</div>
        }
      </div>

      <div class="d-flex flex-wrap align-items-center gap-2">
        <div class="btn-group" role="group" aria-label="Scegli il mese">
          <button type="button" class="btn btn-outline-secondary btn-sm" aria-label="Mese precedente"
                  (click)="mesePrecedente()">
            <i class="bi bi-chevron-left"></i>
          </button>
          <span class="btn btn-light btn-sm text-capitalize disabled fw-semibold" style="min-width: 8.5rem">
            {{ dataSelezionata() | date: 'LLLL y' }}
          </span>
          <button type="button" class="btn btn-outline-secondary btn-sm" aria-label="Mese successivo"
                  [disabled]="eMeseCorrente()" (click)="meseSuccessivo()">
            <i class="bi bi-chevron-right"></i>
          </button>
        </div>
        @if (!eMeseCorrente()) {
          <button type="button" class="btn btn-link btn-sm" (click)="vaiAlMeseCorrente()">Torna a oggi</button>
        }
        @if (spaziLavoro.puoScrivere()) {
          <a class="btn btn-primary btn-sm" routerLink="/transazioni/nuova">
            <i class="bi bi-plus-lg me-1"></i>Nuova transazione
          </a>
        }
      </div>
    </div>

    @if (errore()) {
      <div class="alert alert-danger d-flex justify-content-between align-items-center">
        <span>{{ errore() }}</span>
        <button type="button" class="btn btn-sm btn-outline-danger" (click)="riprova()">Riprova</button>
      </div>
    }

    @if (caricamento() && !riepilogo()) {
      <div class="text-center text-secondary py-5">
        <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
      </div>
    }

    @if (riepilogo(); as dati) {
      <div [class.opacity-50]="caricamento()">
        <div class="row g-3 mb-3">
          <div class="col-6 col-lg-3">
            <app-scheda-kpi titolo="Saldo totale" icona="bi-wallet2"
                            [classeValore]="dati.saldoTotale >= 0 ? 'text-success' : 'text-danger'"
                            nota="Entrate meno uscite, da sempre">
              {{ dati.saldoTotale | currency: valuta() }}
            </app-scheda-kpi>
          </div>
          <div class="col-6 col-lg-3">
            <app-scheda-kpi titolo="Entrate del mese" icona="bi-arrow-down-circle" classeValore="text-success">
              {{ dati.entrateMese | currency: valuta() }}
            </app-scheda-kpi>
          </div>
          <div class="col-6 col-lg-3">
            <app-scheda-kpi titolo="Uscite del mese" icona="bi-arrow-up-circle" classeValore="text-danger">
              {{ dati.usciteMese | currency: valuta() }}
            </app-scheda-kpi>
          </div>
          <div class="col-6 col-lg-3">
            <app-scheda-kpi titolo="Risparmio del mese" icona="bi-piggy-bank"
                            [classeValore]="dati.risparmioMese >= 0 ? 'text-success' : 'text-danger'"
                            nota="Entrate meno uscite del mese">
              {{ dati.risparmioMese | currency: valuta() }}
            </app-scheda-kpi>
          </div>
        </div>

        <div class="row g-3 mb-3">
          <div class="col-12 col-lg-4">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <span class="text-secondary small"><i class="bi bi-pie-chart me-2"></i>Budget utilizzato</span>
                  @if (dati.budget.totale > 0) {
                    <span class="fw-semibold">{{ dati.budget.percentuale | number: '1.0-1' }}%</span>
                  }
                </div>
                @if (dati.budget.totale > 0) {
                  <div class="progress" style="height: 10px">
                    <div class="progress-bar" role="progressbar" [class]="classeBudget()"
                         [style.width.%]="percentualeBudget()"></div>
                  </div>
                  <div class="small text-secondary mt-2">
                    {{ dati.budget.speso | currency: valuta() }} su {{ dati.budget.totale | currency: valuta() }}
                  </div>
                } @else {
                  <div class="small text-secondary">Nessun budget impostato per questo mese.</div>
                }
              </div>
            </div>
          </div>

          <div class="col-12 col-lg-4">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <span class="text-secondary small"><i class="bi bi-bullseye me-2"></i>Obiettivi di risparmio</span>
                  @if (dati.obiettivi.totali > 0) {
                    <span class="fw-semibold">{{ percentualeObiettivi() | number: '1.0-1' }}%</span>
                  }
                </div>
                @if (dati.obiettivi.totali > 0) {
                  <div class="progress" style="height: 10px">
                    <div class="progress-bar bg-primary" role="progressbar"
                         [style.width.%]="percentualeObiettivi()"></div>
                  </div>
                  <div class="small text-secondary mt-2">
                    {{ dati.obiettivi.importoRisparmiato | currency: valuta() }} su
                    {{ dati.obiettivi.importoObiettivi | currency: valuta() }}
                    · {{ dati.obiettivi.completati }} di {{ dati.obiettivi.totali }} raggiunti
                  </div>
                } @else {
                  <div class="small text-secondary">Non hai ancora creato obiettivi di risparmio.</div>
                }
              </div>
            </div>
          </div>

          <div class="col-12 col-lg-4">
            <app-scheda-kpi titolo="Transazioni del mese" icona="bi-receipt">
              {{ dati.transazioniMese }}
            </app-scheda-kpi>
          </div>
        </div>

        <app-grafici-andamento />

        <div class="row g-3 mb-3">
          <div class="col-12 col-lg-6">
            <app-grafico-spese-categoria [mese]="mese()" [anno]="anno()" />
          </div>
          <div class="col-12 col-lg-6">
            <app-grafico-budget [mese]="mese()" [anno]="anno()" />
          </div>
        </div>
        <div class="row g-3">
          <div class="col-12 col-lg-5">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-header bg-white fw-semibold">Categorie con maggiore spesa</div>
              @if (dati.categorieConPiuSpesa.length === 0) {
                <div class="card-body text-secondary small">Nessuna spesa in questo mese.</div>
              } @else {
                <ul class="list-group list-group-flush">
                  @for (categoria of dati.categorieConPiuSpesa; track categoria.categoriaId) {
                    <li class="list-group-item">
                      <div class="d-flex justify-content-between align-items-center mb-1">
                        <span>
                          <span class="d-inline-block rounded-circle me-2" style="width: 0.65rem; height: 0.65rem"
                                [style.background-color]="categoria.colore ?? '#6c757d'"></span>
                          {{ categoria.categoriaNome }}
                        </span>
                        <span class="fw-semibold">{{ categoria.totale | currency: valuta() }}</span>
                      </div>
                      <div class="progress" style="height: 6px">
                        <div class="progress-bar" role="progressbar" [style.width.%]="categoria.percentuale"
                             [style.background-color]="categoria.colore ?? '#6c757d'"></div>
                      </div>
                      <div class="small text-secondary mt-1">
                        {{ categoria.percentuale | number: '1.0-1' }}% delle uscite
                      </div>
                    </li>
                  }
                </ul>
              }
            </div>
          </div>

          <div class="col-12 col-lg-7">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <span class="fw-semibold">Ultime transazioni</span>
                <a routerLink="/transazioni" class="small">Vedi tutte</a>
              </div>
              @if (dati.ultimeTransazioni.length === 0) {
                <div class="card-body text-secondary small">
                  Non ci sono ancora transazioni.
                  @if (spaziLavoro.puoScrivere()) {
                    <a routerLink="/transazioni/nuova">Aggiungi la prima</a>
                  }
                </div>
              } @else {
                <div class="table-responsive">
                  <table class="table table-sm align-middle mb-0">
                    <tbody>
                      @for (transazione of dati.ultimeTransazioni; track transazione.id) {
                        <tr>
                          <td class="text-nowrap text-secondary small ps-3">
                            {{ transazione.data | date: 'dd/MM' }}
                          </td>
                          <td>
                            {{ transazione.descrizione }}
                            <div class="small text-secondary">{{ transazione.categoriaNome }}</div>
                          </td>
                          <td class="text-end text-nowrap fw-semibold pe-3"
                              [class.text-success]="transazione.tipo === 'ENTRATA'"
                              [class.text-danger]="transazione.tipo === 'USCITA'">
                            {{ transazione.tipo === 'USCITA' ? '−' : '+' }}{{ transazione.importo | currency: valuta() }}
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class DashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  private readonly oggi = new Date();
  readonly mese = signal(this.oggi.getMonth() + 1);
  readonly anno = signal(this.oggi.getFullYear());

  readonly riepilogo = signal<RiepilogoDashboard | null>(null);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');

  /** Il primo giorno del mese scelto: serve a scriverne il nome ("ottobre 2026") */
  readonly dataSelezionata = computed(() => new Date(this.anno(), this.mese() - 1, 1));
  readonly eMeseCorrente = computed(
    () => this.mese() === this.oggi.getMonth() + 1 && this.anno() === this.oggi.getFullYear(),
  );

  /** La barra non va oltre il 100%, anche se il budget è stato superato */
  readonly percentualeBudget = computed(() => Math.min(this.riepilogo()?.budget.percentuale ?? 0, 100));
  /** Stesse soglie del backend: verde sotto l'80%, giallo dall'80%, rosso dal 100% */
  readonly classeBudget = computed(() => {
    const percentuale = this.riepilogo()?.budget.percentuale ?? 0;
    if (percentuale >= 100) {
      return 'bg-danger';
    }
    return percentuale >= 80 ? 'bg-warning' : 'bg-success';
  });

  readonly percentualeObiettivi = computed(() => {
    const obiettivi = this.riepilogo()?.obiettivi;
    if (!obiettivi || obiettivi.importoObiettivi <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((obiettivi.importoRisparmiato / obiettivi.importoObiettivi) * 1000) / 10);
  });

  private richiestaInCorso?: Subscription;

  constructor() {
    // Si ricarica quando cambia lo spazio attivo o il mese scelto
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      const mese = this.mese();
      const anno = this.anno();
      if (spazioId === null) {
        return;
      }
      untracked(() => this.carica(spazioId, mese, anno));
    });
  }

  mesePrecedente(): void {
    if (this.mese() === 1) {
      this.mese.set(12);
      this.anno.update((anno) => anno - 1);
    } else {
      this.mese.update((mese) => mese - 1);
    }
  }

  meseSuccessivo(): void {
    if (this.mese() === 12) {
      this.mese.set(1);
      this.anno.update((anno) => anno + 1);
    } else {
      this.mese.update((mese) => mese + 1);
    }
  }

  vaiAlMeseCorrente(): void {
    this.mese.set(this.oggi.getMonth() + 1);
    this.anno.set(this.oggi.getFullYear());
  }

  riprova(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId !== null) {
      this.carica(spazioId, this.mese(), this.anno());
    }
  }

  private carica(spazioId: number, mese: number, anno: number): void {
    // Se parte una nuova richiesta, la precedente non serve più
    this.richiestaInCorso?.unsubscribe();
    this.caricamento.set(true);
    this.errore.set(null);

    this.richiestaInCorso = this.dashboardService.riepilogo(spazioId, mese, anno).subscribe({
      next: (riepilogo) => {
        this.riepilogo.set(riepilogo);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
