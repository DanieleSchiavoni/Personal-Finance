// src/app/shared/componenti/elenco-notizie/elenco-notizie.component.ts
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';

import { NotizieService } from '../../../core/services/notizie.service';
import { Notizia } from '../../../models/notizia.model';

/** Griglia di notizie con filtro per testata. Si usa sia nella home pubblica sia nell'area riservata. */
@Component({
  selector: 'app-elenco-notizie',
  imports: [DatePipe],
  template: `
    <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
      <button type="button" class="btn btn-sm rounded-pill"
              [class.btn-primary]="fonteScelta() === null" [class.btn-outline-secondary]="fonteScelta() !== null"
              (click)="fonteScelta.set(null)">Tutte</button>
      @for (fonte of fonti(); track fonte) {
        <button type="button" class="btn btn-sm rounded-pill"
                [class.btn-primary]="fonteScelta() === fonte" [class.btn-outline-secondary]="fonteScelta() !== fonte"
                (click)="fonteScelta.set(fonte)">{{ fonte }}</button>
      }
      <button type="button" class="btn btn-sm btn-light ms-auto" [disabled]="caricamento()" (click)="carica()">
        <i class="bi bi-arrow-clockwise me-1"></i>Aggiorna
      </button>
    </div>

    @if (caricamento()) {
      <div class="text-center text-secondary py-5">
        <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento delle notizie…
      </div>
    } @else if (errore()) {
      <div class="alert alert-warning">{{ errore() }}</div>
    } @else if (visibili().length === 0) {
      <div class="alert alert-light border">
        Al momento non ci sono notizie da mostrare. Riprova tra qualche minuto.
      </div>
    } @else {
      <div class="row g-3">
        @for (notizia of visibili(); track notizia.url) {
          <div class="col-12 col-md-6 col-xl-4">
            <article class="card h-100 border-0 shadow-sm">
              @if (notizia.immagineUrl) {
                <img [src]="notizia.immagineUrl" alt="" class="card-img-top immagine-notizia" loading="lazy"
                     referrerpolicy="no-referrer"
                     (error)="nascondiImmagine($event)" />
              }
              <div class="card-body d-flex flex-column">
                <div class="d-flex flex-wrap align-items-center gap-2 mb-2 small">
                  <span class="badge text-bg-primary">{{ notizia.fonte }}</span>
                  <span class="badge text-bg-light border text-uppercase">{{ notizia.lingua }}</span>
                  @if (notizia.pubblicataIl) {
                    <span class="text-secondary">{{ notizia.pubblicataIl | date: 'd MMM y, HH:mm' }}</span>
                  }
                </div>
                <h3 class="h6">
                  <a [href]="notizia.url" target="_blank" rel="noopener noreferrer"
                     class="text-dark text-decoration-none stretched-link">{{ notizia.titolo }}</a>
                </h3>
                @if (notizia.sintesi) {
                  <p class="small text-secondary mb-3">{{ notizia.sintesi }}</p>
                }
                <div class="mt-auto small text-primary">
                  Leggi su {{ notizia.fonte }} <i class="bi bi-box-arrow-up-right ms-1"></i>
                </div>
              </div>
            </article>
          </div>
        }
      </div>
    }
  `,
})
export class ElencoNotizieComponent implements OnInit {
  private readonly servizio = inject(NotizieService);

  readonly limite = input(24);

  readonly notizie = signal<Notizia[]>([]);
  readonly caricamento = signal(true);
  readonly errore = signal<string | null>(null);
  readonly fonteScelta = signal<string | null>(null);

  readonly fonti = computed(() => [...new Set(this.notizie().map((notizia) => notizia.fonte))]);
  readonly visibili = computed(() => {
    const fonte = this.fonteScelta();
    return fonte ? this.notizie().filter((notizia) => notizia.fonte === fonte) : this.notizie();
  });

  ngOnInit(): void {
    this.carica();
  }

  carica(): void {
    this.caricamento.set(true);
    this.errore.set(null);
    this.servizio.ultime(this.limite()).subscribe({
      next: (notizie) => {
        this.notizie.set(notizie);
        // Se la testata scelta non c'è più nell'elenco aggiornato, si torna a "Tutte"
        const scelta = this.fonteScelta();
        if (scelta && !notizie.some((notizia) => notizia.fonte === scelta)) {
          this.fonteScelta.set(null);
        }
        this.caricamento.set(false);
      },
      error: () => {
        this.errore.set('Non è stato possibile recuperare le notizie. Controlla che il backend sia avviato.');
        this.caricamento.set(false);
      },
    });
  }

  /** Se l'immagine non si carica la si nasconde, senza lasciare un buco */
  nascondiImmagine(evento: Event): void {
    (evento.target as HTMLImageElement).style.display = 'none';
  }
}
