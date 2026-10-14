// src/app/features/admin/admin-statistiche.component.ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { AdminService } from '../../core/services/admin.service';
import { StatisticheGenerali } from '../../models/admin.model';
import { messaggioErrore } from '../../shared/utilita/errori';

@Component({
  selector: 'app-admin-statistiche',
  template: `
    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }
    @if (caricamento()) {
      <div class="text-secondary"><span class="spinner-border spinner-border-sm me-2"></span>Caricamento…</div>
    }
    <div class="row g-3">
      @for (voce of voci(); track voce.etichetta) {
        <div class="col-6 col-lg-3">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-body">
              <div class="text-secondary small"><i class="bi {{ voce.icona }} me-1"></i>{{ voce.etichetta }}</div>
              <div class="fs-3 fw-semibold">{{ voce.valore }}</div>
            </div>
          </div>
        </div>
      }
    </div>
    <p class="small text-secondary mt-3 mb-0">
      Le statistiche sono solo conteggi: gli amministratori non vedono i dati finanziari degli utenti.
    </p>
  `,
})
export class AdminStatisticheComponent implements OnInit {
  private readonly admin = inject(AdminService);

  readonly statistiche = signal<StatisticheGenerali | null>(null);
  readonly caricamento = signal(true);
  readonly errore = signal<string | null>(null);

  readonly voci = computed(() => {
    const s = this.statistiche();
    if (!s) {
      return [];
    }
    return [
      { etichetta: 'Utenti', valore: s.utentiTotali, icona: 'bi-people' },
      { etichetta: 'Utenti abilitati', valore: s.utentiAbilitati, icona: 'bi-person-check' },
      { etichetta: 'Utenti bloccati', valore: s.utentiBloccati, icona: 'bi-person-slash' },
      { etichetta: 'Amministratori', valore: s.amministratori, icona: 'bi-shield-lock' },
      { etichetta: 'Spazi personali', valore: s.spaziPersonali, icona: 'bi-person-circle' },
      { etichetta: 'Spazi aziendali', valore: s.spaziAziendali, icona: 'bi-building' },
      { etichetta: 'Transazioni', valore: s.transazioniTotali, icona: 'bi-arrow-left-right' },
      { etichetta: 'Categorie globali', valore: s.categorieGlobali, icona: 'bi-tags' },
    ];
  });

  ngOnInit(): void {
    this.admin.statistiche().subscribe({
      next: (statistiche) => {
        this.statistiche.set(statistiche);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
