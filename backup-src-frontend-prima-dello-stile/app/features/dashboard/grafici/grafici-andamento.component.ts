// src/app/features/dashboard/grafici/grafici-andamento.component.ts
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription } from 'rxjs';

import { DashboardService } from '../../../core/services/dashboard.service';
import { SpazioLavoroService } from '../../../core/services/spazio-lavoro.service';
import { Granularita, PuntoTrend } from '../../../models/dashboard.model';
import { COLORI_GRAFICI } from '../../../shared/utilita/colori-grafici';
import { dataDaIso, formattaIso } from '../../../shared/utilita/date';
import { messaggioErrore } from '../../../shared/utilita/errori';
import { formattaValuta } from '../../../shared/utilita/valuta';

/** Quanti periodi si possono mostrare, per ciascun tipo di raggruppamento */
const OPZIONI_PERIODI: Record<Granularita, number[]> = {
  MESE: [6, 12, 24],
  SETTIMANA: [8, 12, 26],
};

function etichettaMese(iso: string): string {
  return dataDaIso(iso).toLocaleDateString('it-IT', { month: 'short', year: '2-digit' });
}

function etichettaGiornoMese(iso: string): string {
  return dataDaIso(iso).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' });
}

function titoloMese(iso: string): string {
  const testo = dataDaIso(iso).toLocaleDateString('it-IT', { month: 'long', year: 'numeric' });
  return testo.charAt(0).toUpperCase() + testo.slice(1);
}

@Component({
  selector: 'app-grafici-andamento',
  imports: [BaseChartDirective],
  template: `
    <div class="card border-0 shadow-sm mb-3">
      <div class="card-header bg-white d-flex flex-wrap justify-content-between align-items-center gap-2">
        <span class="fw-semibold">Andamento</span>
        <div class="d-flex flex-wrap align-items-center gap-2">
          <div class="btn-group btn-group-sm" role="group" aria-label="Raggruppa per">
            <button type="button" class="btn"
                    [class.btn-primary]="granularita() === 'SETTIMANA'"
                    [class.btn-outline-secondary]="granularita() !== 'SETTIMANA'"
                    (click)="cambiaGranularita('SETTIMANA')">Settimane</button>
            <button type="button" class="btn"
                    [class.btn-primary]="granularita() === 'MESE'"
                    [class.btn-outline-secondary]="granularita() !== 'MESE'"
                    (click)="cambiaGranularita('MESE')">Mesi</button>
          </div>
          <select class="form-select form-select-sm w-auto" aria-label="Quanti periodi mostrare"
                  (change)="cambiaPeriodi($event)">
            @for (n of opzioniPeriodi(); track n) {
              <option [value]="n" [selected]="n === periodi()">Ultimi {{ n }} {{ unita() }}</option>
            }
          </select>
        </div>
      </div>

      <div class="card-body">
        @if (errore()) {
          <div class="alert alert-danger mb-0">{{ errore() }}</div>
        } @else if (caricamento() && punti().length === 0) {
          <div class="text-center text-secondary py-5">
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
          </div>
        } @else if (punti().length === 0) {
          <div class="text-secondary small">Nessun dato da mostrare.</div>
        } @else {
          <div class="row g-4" [class.opacity-50]="caricamento()">
            <div class="col-12 col-xl-7">
              <div class="small text-secondary mb-2">Entrate e uscite</div>
              <div class="contenitore-grafico">
                <canvas baseChart type="bar" [data]="datiBarre()" [options]="opzioniBarre()"
                        role="img" aria-label="Grafico a barre delle entrate e delle uscite per periodo"></canvas>
              </div>
            </div>
            <div class="col-12 col-xl-5">
              <div class="small text-secondary mb-2">Andamento del saldo</div>
              <div class="contenitore-grafico">
                <canvas baseChart type="line" [data]="datiLinea()" [options]="opzioniLinea()"
                        role="img" aria-label="Grafico a linea del saldo nel tempo"></canvas>
              </div>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`.contenitore-grafico { position: relative; height: 280px; }`],
})
export class GraficiAndamentoComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly spaziLavoro = inject(SpazioLavoroService);

  readonly granularita = signal<Granularita>('MESE');
  readonly periodi = signal(12);

  readonly punti = signal<PuntoTrend[]>([]);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  readonly opzioniPeriodi = computed(() => OPZIONI_PERIODI[this.granularita()]);
  readonly unita = computed(() => (this.granularita() === 'MESE' ? 'mesi' : 'settimane'));

  private readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');

  /** Le etichette sull'asse: "ott 26" per i mesi, "05/10" (primo giorno) per le settimane */
  private readonly etichette = computed(() =>
    this.punti().map((punto) =>
      this.granularita() === 'MESE' ? etichettaMese(punto.inizio) : etichettaGiornoMese(punto.inizio),
    ),
  );

  /** Il titolo che compare passando il mouse su un periodo */
  private readonly titoli = computed(() =>
    this.punti().map((punto) =>
      this.granularita() === 'MESE'
        ? titoloMese(punto.inizio)
        : `Settimana dal ${etichettaGiornoMese(punto.inizio)} al ${etichettaGiornoMese(punto.fine)}`,
    ),
  );

  readonly datiBarre = computed<ChartData<'bar'>>(() => ({
    labels: this.etichette(),
    datasets: [
      {
        label: 'Entrate',
        data: this.punti().map((punto) => punto.entrate),
        backgroundColor: COLORI_GRAFICI.entrate,
        borderRadius: 4,
      },
      {
        label: 'Uscite',
        data: this.punti().map((punto) => punto.uscite),
        backgroundColor: COLORI_GRAFICI.uscite,
        borderRadius: 4,
      },
    ],
  }));

  readonly datiLinea = computed<ChartData<'line'>>(() => ({
    labels: this.etichette(),
    datasets: [
      {
        label: 'Saldo',
        data: this.punti().map((punto) => punto.saldoProgressivo),
        borderColor: COLORI_GRAFICI.saldo,
        backgroundColor: COLORI_GRAFICI.saldoSfondo,
        fill: true,
        tension: 0.3,
        pointRadius: 3,
      },
    ],
  }));

  readonly opzioniBarre = computed<ChartConfiguration<'bar'>['options']>(() => {
    const valuta = this.valuta();
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            title: (voci) => this.titoli()[voci[0].dataIndex] ?? '',
            label: (contesto) => ` ${contesto.dataset.label}: ${formattaValuta(contesto.parsed.y ?? 0, valuta)}`,
          },
        },
      },
      scales: {
        y: { beginAtZero: true, ticks: { callback: (valore) => formattaValuta(Number(valore), valuta) } },
      },
    };
  });

  readonly opzioniLinea = computed<ChartConfiguration<'line'>['options']>(() => {
    const valuta = this.valuta();
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            title: (voci) => this.titoli()[voci[0].dataIndex] ?? '',
            label: (contesto) => ` Saldo: ${formattaValuta(contesto.parsed.y ?? 0, valuta)}`,
          },
        },
      },
      // Il saldo può essere negativo: l'asse non parte per forza da zero
      scales: { y: { ticks: { callback: (valore) => formattaValuta(Number(valore), valuta) } } },
    };
  });

  private richiestaInCorso?: Subscription;

  constructor() {
    // Si ricarica quando cambia lo spazio attivo, il raggruppamento o il numero di periodi
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      const granularita = this.granularita();
      const periodi = this.periodi();
      if (spazioId === null) {
        return;
      }
      untracked(() => this.carica(spazioId, granularita, periodi));
    });
  }

  cambiaGranularita(granularita: Granularita): void {
    this.granularita.set(granularita);
    this.periodi.set(12); // 12 è previsto sia per i mesi sia per le settimane
  }

  cambiaPeriodi(evento: Event): void {
    this.periodi.set(Number((evento.target as HTMLSelectElement).value));
  }

  private carica(spazioId: number, granularita: Granularita, periodi: number): void {
    this.richiestaInCorso?.unsubscribe();
    this.caricamento.set(true);
    this.errore.set(null);

    this.richiestaInCorso = this.dashboardService
      .trend(spazioId, granularita, this.calcolaDal(granularita, periodi))
      .subscribe({
        next: (punti) => {
          this.punti.set(punti);
          this.caricamento.set(false);
        },
        error: (errore: unknown) => {
          this.errore.set(messaggioErrore(errore));
          this.caricamento.set(false);
        },
      });
  }

  /** Da quando partire per mostrare gli ultimi N periodi (il backend allinea a mesi e settimane intere) */
  private calcolaDal(granularita: Granularita, periodi: number): string {
    const oggi = new Date();
    const dal =
      granularita === 'MESE'
        ? new Date(oggi.getFullYear(), oggi.getMonth() - (periodi - 1), 1)
        : new Date(oggi.getFullYear(), oggi.getMonth(), oggi.getDate() - 7 * (periodi - 1));
    return formattaIso(dal);
  }
}
