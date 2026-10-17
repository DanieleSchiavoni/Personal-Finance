// src/app/shared/componenti/selettore-mese/selettore-mese.component.ts
import { DatePipe } from '@angular/common';
import { Component, computed, input, model } from '@angular/core';

/**
 * Frecce per scegliere un mese. Si usa con il collegamento a due vie ai segnali della pagina:
 * <app-selettore-mese [(mese)]="mese" [(anno)]="anno" />
 */
@Component({
  selector: 'app-selettore-mese',
  imports: [DatePipe],
  template: `
    <div class="d-flex flex-wrap align-items-center gap-2">
      <div class="btn-group" role="group" aria-label="Scegli il mese">
        <button type="button" class="btn btn-outline-secondary btn-sm" aria-label="Mese precedente"
                (click)="precedente()">
          <i class="bi bi-chevron-left"></i>
        </button>
        <span class="btn btn-light btn-sm text-capitalize disabled fw-semibold" style="min-width: 8.5rem">
          {{ data() | date: 'LLLL y' }}
        </span>
        <button type="button" class="btn btn-outline-secondary btn-sm" aria-label="Mese successivo"
                [disabled]="successivoBloccato()" (click)="successivo()">
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>
      @if (!eMeseCorrente()) {
        <button type="button" class="btn btn-link btn-sm" (click)="tornaAOggi()">Torna a oggi</button>
      }
    </div>
  `,
})
export class SelettoreMeseComponent {
  readonly mese = model.required<number>();
  readonly anno = model.required<number>();
  /** Se false non si può andare oltre il mese corrente */
  readonly consentiFuturo = input(false);

  private readonly adesso = new Date();

  readonly data = computed(() => new Date(this.anno(), this.mese() - 1, 1));
  readonly eMeseCorrente = computed(
    () => this.mese() === this.adesso.getMonth() + 1 && this.anno() === this.adesso.getFullYear(),
  );
  readonly successivoBloccato = computed(() => !this.consentiFuturo() && this.eMeseCorrente());

  precedente(): void {
    if (this.mese() === 1) {
      this.mese.set(12);
      this.anno.update((anno) => anno - 1);
    } else {
      this.mese.update((mese) => mese - 1);
    }
  }

  successivo(): void {
    if (this.mese() === 12) {
      this.mese.set(1);
      this.anno.update((anno) => anno + 1);
    } else {
      this.mese.update((mese) => mese + 1);
    }
  }

  tornaAOggi(): void {
    this.mese.set(this.adesso.getMonth() + 1);
    this.anno.set(this.adesso.getFullYear());
  }
}
