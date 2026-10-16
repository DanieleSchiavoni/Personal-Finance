// src/app/shared/componenti/scheda-kpi/scheda-kpi.component.ts
import { Component, input } from '@angular/core';

/** Una scheda con un titolo, un'icona e un valore (il contenuto scritto dentro il tag) */
@Component({
  selector: 'app-scheda-kpi',
  template: `
    <div class="card border-0 shadow-sm h-100">
      <div class="card-body">
        <div class="d-flex align-items-center gap-2 text-secondary small mb-1">
          <i class="bi" [class]="icona()"></i>
          <span>{{ titolo() }}</span>
        </div>
        <div class="fs-4 fw-semibold" [class]="classeValore()"><ng-content /></div>
        @if (nota()) {
          <div class="small text-secondary mt-1">{{ nota() }}</div>
        }
      </div>
    </div>
  `,
})
export class SchedaKpiComponent {
  readonly titolo = input.required<string>();
  readonly icona = input('bi-circle');
  /** Classi Bootstrap per colorare il valore, ad esempio text-success */
  readonly classeValore = input('');
  readonly nota = input('');
}
