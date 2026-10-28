// src/app/features/notifiche/notifiche.component.ts
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { NotificaService } from '../../core/services/notifica.service';
import { Notifica } from '../../models/notifica.model';
import { messaggioErrore } from '../../shared/utilita/errori';

@Component({
  selector: 'app-notifiche',
  imports: [DatePipe],
  template: `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <h1 class="h3 mb-0">Notifiche</h1>
      <button type="button" class="btn btn-outline-secondary btn-sm"
              [disabled]="nonLette() === 0" (click)="segnaTutte()">
        <i class="bi bi-check2-all me-1"></i>Segna tutte come lette
      </button>
    </div>

    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    @if (caricamento()) {
      <div class="text-secondary"><span class="spinner-border spinner-border-sm me-2"></span>Caricamento…</div>
    } @else if (elenco().length === 0) {
      <div class="card border-0 shadow-sm">
        <div class="card-body text-center text-secondary py-5">
          <i class="bi bi-bell-slash fs-3 d-block mb-2"></i>Non hai notifiche.
        </div>
      </div>
    } @else {
      <div class="d-flex flex-column gap-2 larghezza-max-760">
        @for (notifica of elenco(); track notifica.id) {
          <div class="card border-0 shadow-sm" [class.border-start]="!notifica.letta"
               [class.border-primary]="!notifica.letta" [class.border-4]="!notifica.letta">
            <div class="card-body d-flex gap-3 align-items-start">
              <div class="flex-grow-1">
                <div [class.fw-semibold]="!notifica.letta">{{ notifica.titolo }}</div>
                <div class="text-secondary small">{{ notifica.messaggio }}</div>
                <div class="text-secondary small mt-1">{{ notifica.data | date: 'dd/MM/yyyy HH:mm' }}</div>
              </div>
              @if (!notifica.letta) {
                <button type="button" class="btn btn-sm btn-outline-primary" title="Segna come letta"
                        (click)="segnaComeLetta(notifica)"><i class="bi bi-check2"></i></button>
              }
              <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                      (click)="elimina(notifica)"><i class="bi bi-trash"></i></button>
            </div>
          </div>
        }
      </div>
    }
  `,
})
export class NotificheComponent implements OnInit {
  private readonly servizio = inject(NotificaService);

  readonly elenco = signal<Notifica[]>([]);
  readonly nonLette = computed(() => this.elenco().filter((n) => !n.letta).length);
  readonly caricamento = signal(true);
  readonly errore = signal<string | null>(null);

  ngOnInit(): void {
    this.servizio.elenca().subscribe({
      next: (notifiche) => {
        this.elenco.set(notifiche);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }

  segnaComeLetta(notifica: Notifica): void {
    this.servizio.segnaComeLetta(notifica.id).subscribe({
      next: (aggiornata) => {
        this.elenco.update((lista) => lista.map((n) => (n.id === aggiornata.id ? aggiornata : n)));
        this.servizio.nonLette.set(this.nonLette());
      },
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }

  segnaTutte(): void {
    this.servizio.segnaTutteComeLette().subscribe({
      next: () => {
        this.elenco.update((lista) => lista.map((n) => ({ ...n, letta: true })));
        this.servizio.nonLette.set(0);
      },
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }

  elimina(notifica: Notifica): void {
    this.servizio.elimina(notifica.id).subscribe({
      next: () => {
        this.elenco.update((lista) => lista.filter((n) => n.id !== notifica.id));
        this.servizio.nonLette.set(this.nonLette());
      },
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }
}
