import { Component, inject, input } from '@angular/core';

import { TemaService } from '../../../core/services/tema.service';

/** Il pulsante che passa dal tema chiaro a quello scuro e viceversa */
@Component({
  selector: 'app-pulsante-tema',
  template: `
    <button type="button" class="btn btn-outline-secondary btn-sm" [class.w-100]="conEtichetta()"
            [attr.aria-label]="tema.scuro() ? 'Passa al tema chiaro' : 'Passa al tema scuro'"
            [title]="tema.scuro() ? 'Passa al tema chiaro' : 'Passa al tema scuro'"
            (click)="tema.inverti()">
      <i class="bi" [class.bi-moon-stars]="!tema.scuro()" [class.bi-sun]="tema.scuro()"></i>
      @if (conEtichetta()) {
        <span class="ms-1">{{ tema.scuro() ? 'Tema chiaro' : 'Tema scuro' }}</span>
      }
    </button>
  `,
})
export class PulsanteTemaComponent {
  protected readonly tema = inject(TemaService);

  /** Con l'etichetta il pulsante occupa tutta la larghezza (barra laterale); senza, è solo l'icona */
  readonly conEtichetta = input(false);
}
