// src/app/shared/componenti/segnaposto/segnaposto.component.ts
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

/** Pagina provvisoria per le sezioni che verranno realizzate nelle fasi successive */
@Component({
  selector: 'app-segnaposto',
  template: `
    <h1 class="h3 mb-3">{{ titolo() }}</h1>
    <div class="card border-0 shadow-sm">
      <div class="card-body text-secondary">
        <i class="bi bi-tools me-2"></i>Questa pagina verrà realizzata nelle prossime fasi.
      </div>
    </div>
  `,
})
export class SegnapostoComponent {
  readonly titolo = toSignal(inject(ActivatedRoute).data.pipe(map((dati) => dati['titolo'] as string)));
}
