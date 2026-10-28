// src/app/features/dashboard/grafici/grafico-spese-categoria.component.ts
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription } from 'rxjs';

import { DashboardService } from '../../../core/services/dashboard.service';
import { SpazioLavoroService } from '../../../core/services/spazio-lavoro.service';
import { TotaleCategoria } from '../../../models/dashboard.model';
import { COLORI_GRAFICI, coloreSuperficie, opzioniTema } from '../../../shared/utilita/colori-grafici';
import { TemaService } from '../../../core/services/tema.service';
import { formattaIso } from '../../../shared/utilita/date';
import { messaggioErrore } from '../../../shared/utilita/errori';
import { formattaValuta } from '../../../shared/utilita/valuta';

@Component({
  selector: 'app-grafico-spese-categoria',
  imports: [BaseChartDirective],
  template: `
    <div class="card border-0 shadow-sm h-100">
      <div class="card-header bg-white fw-semibold">Spese per categoria</div>
      <div class="card-body">
        @if (errore()) {
          <div class="alert alert-danger mb-0">{{ errore() }}</div>
        } @else if (caricamento() && voci().length === 0) {
          <div class="text-center text-secondary py-5">
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
          </div>
        } @else if (voci().length === 0) {
          <div class="text-secondary small">Nessuna spesa in questo mese.</div>
        } @else {
          <div class="contenitore-grafico contenitore-grafico-alto" [class.opacity-50]="caricamento()">
            <canvas baseChart type="doughnut" [data]="dati()" [options]="opzioni()"
                    role="img" aria-label="Grafico a ciambella delle spese per categoria"></canvas>
          </div>
        }
      </div>
    </div>
  `,
})
export class GraficoSpeseCategoriaComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly spaziLavoro = inject(SpazioLavoroService);
  private readonly tema = inject(TemaService);

  readonly mese = input.required<number>();
  readonly anno = input.required<number>();

  readonly voci = signal<TotaleCategoria[]>([]);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  private readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');

  readonly dati = computed<ChartData<'doughnut'>>(() => ({
    labels: this.voci().map((voce) => voce.categoriaNome),
    datasets: [
      {
        data: this.voci().map((voce) => voce.totale),
        backgroundColor: this.voci().map((voce) => voce.colore ?? COLORI_GRAFICI.neutro),
        borderWidth: 2,
        borderColor: coloreSuperficie(this.tema.scuro()),
      },
    ],
  }));

  readonly opzioni = computed<ChartConfiguration<'doughnut'>['options']>(() => {
    const valuta = this.valuta();
    return {
      ...opzioniTema(this.tema.scuro()),
      responsive: true,
      maintainAspectRatio: false,
      cutout: '60%',
      plugins: {
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            label: (contesto) => {
              const voce = this.voci()[contesto.dataIndex];
              const percentuale = voce ? ` (${voce.percentuale}%)` : '';
              return ` ${contesto.label}: ${formattaValuta(Number(contesto.parsed), valuta)}${percentuale}`;
            },
          },
        },
      },
    };
  });

  private richiestaInCorso?: Subscription;

  constructor() {
    // Si ricarica quando cambia lo spazio attivo o il mese scelto nella dashboard
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

  private carica(spazioId: number, mese: number, anno: number): void {
    this.richiestaInCorso?.unsubscribe();
    this.caricamento.set(true);
    this.errore.set(null);

    const dal = formattaIso(new Date(anno, mese - 1, 1));
    const al = formattaIso(new Date(anno, mese, 0)); // giorno 0 del mese dopo = ultimo giorno di questo mese

    this.richiestaInCorso = this.dashboardService.spesePerCategoria(spazioId, dal, al).subscribe({
      next: (voci) => {
        this.voci.set(voci);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
