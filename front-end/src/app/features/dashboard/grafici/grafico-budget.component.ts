// src/app/features/dashboard/grafici/grafico-budget.component.ts
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription } from 'rxjs';

import { BudgetService } from '../../../core/services/budget.service';
import { SpazioLavoroService } from '../../../core/services/spazio-lavoro.service';
import { Budget, StatoBudget } from '../../../models/budget.model';
import { COLORI_GRAFICI, opzioniTema } from '../../../shared/utilita/colori-grafici';
import { TemaService } from '../../../core/services/tema.service';
import { messaggioErrore } from '../../../shared/utilita/errori';
import { formattaValuta } from '../../../shared/utilita/valuta';

@Component({
  selector: 'app-grafico-budget',
  imports: [BaseChartDirective, RouterLink],
  template: `
    <div class="card border-0 shadow-sm h-100">
      <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <span class="fw-semibold">Utilizzo dei budget</span>
        <a routerLink="/budget" class="small">Gestisci</a>
      </div>
      <div class="card-body">
        @if (errore()) {
          <div class="alert alert-danger mb-0">{{ errore() }}</div>
        } @else if (caricamento() && budget().length === 0) {
          <div class="text-center text-secondary py-5">
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
          </div>
        } @else if (budget().length === 0) {
          <div class="text-secondary small">Nessun budget impostato per questo mese.</div>
        } @else {
          <div class="position-relative altezza-variabile" [style.--altezza]="altezza() + 'px'" [class.opacity-50]="caricamento()">
            <canvas baseChart type="bar" [data]="dati()" [options]="opzioni()"
                    role="img" aria-label="Grafico a barre della percentuale di utilizzo dei budget"></canvas>
          </div>
          <div class="small text-secondary mt-2">
            Verde sotto l'80%, giallo dall'80%, rosso al 100% o oltre.
          </div>
        }
      </div>
    </div>
  `,
})
export class GraficoBudgetComponent {
  private readonly budgetService = inject(BudgetService);
  private readonly spaziLavoro = inject(SpazioLavoroService);
  private readonly tema = inject(TemaService);

  readonly mese = input.required<number>();
  readonly anno = input.required<number>();

  readonly budget = signal<Budget[]>([]);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  private readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');

  /** Altezza del grafico: una riga per budget, con un minimo */
  readonly altezza = computed(() => Math.max(160, this.budget().length * 44 + 60));

  readonly dati = computed<ChartData<'bar'>>(() => ({
    labels: this.budget().map((voce) => voce.categoriaNome),
    datasets: [
      {
        label: 'Utilizzo del budget',
        data: this.budget().map((voce) => voce.percentuale),
        backgroundColor: this.budget().map((voce) => this.coloreStato(voce.stato)),
        borderRadius: 4,
      },
    ],
  }));

  readonly opzioni = computed<ChartConfiguration<'bar'>['options']>(() => {
    const valuta = this.valuta();
    return {
      ...opzioniTema(this.tema.scuro()),
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (contesto) => {
              const voce = this.budget()[contesto.dataIndex];
              if (!voce) {
                return '';
              }
              return ` ${formattaValuta(voce.speso, valuta)} su ${formattaValuta(voce.importo, valuta)} (${voce.percentuale}%)`;
            },
          },
        },
      },
      scales: {
        x: { beginAtZero: true, suggestedMax: 100, ticks: { callback: (valore) => `${valore}%` } },
      },
    };
  });

  private richiestaInCorso?: Subscription;

  constructor() {
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

  private coloreStato(stato: StatoBudget): string {
    switch (stato) {
      case 'SUPERATO':
        return COLORI_GRAFICI.uscite;
      case 'ATTENZIONE':
        return COLORI_GRAFICI.attenzione;
      default:
        return COLORI_GRAFICI.entrate;
    }
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
