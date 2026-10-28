import { Component, input, output } from '@angular/core';

/**
 * Finestra di conferma. Si mostra con @if nel componente che la usa.
 * Il contenuto scritto dentro il tag (ad esempio un campo di testo) compare sotto il messaggio.
 */
@Component({
  selector: 'app-modale-conferma',
  template: `
    <div class="modal-backdrop fade show"></div>
    <div class="modal d-block" tabindex="-1" role="dialog" aria-modal="true" (click)="annullato.emit()">
      <div class="modal-dialog modal-dialog-centered" (click)="$event.stopPropagation()">
        <div class="modal-content border-0 shadow">
          <div class="modal-header border-0 pb-0">
            <h2 class="modal-title h5">{{ titolo() }}</h2>
            <button type="button" class="btn-close" aria-label="Chiudi" (click)="annullato.emit()"></button>
          </div>
          <div class="modal-body">
            <p class="mb-0">{{ messaggio() }}</p>
            <ng-content />
          </div>
          <div class="modal-footer border-0 pt-0">
            <button type="button" class="btn btn-light" (click)="annullato.emit()">Annulla</button>
            <button
              type="button"
              class="btn"
              [class.btn-danger]="stile() === 'danger'"
              [class.btn-primary]="stile() === 'primary'"
              [disabled]="inCorso() || confermaDisabilitata()"
              (click)="confermato.emit()"
            >
              @if (inCorso()) {
                <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
              }
              {{ etichettaConferma() }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ModaleConfermaComponent {
  readonly titolo = input.required<string>();
  readonly messaggio = input('');
  readonly etichettaConferma = input('Conferma');
  /** danger per le azioni che eliminano, primary per le altre */
  readonly stile = input<'danger' | 'primary'>('danger');
  readonly inCorso = input(false);
  readonly confermaDisabilitata = input(false);

  readonly confermato = output<void>();
  readonly annullato = output<void>();
}
