// src/app/features/dashboard/pulsante-resoconto.component.ts
import { Component, inject, input, signal } from '@angular/core';

import { ReportService } from '../../core/services/report.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { scaricaFile } from '../../shared/utilita/download';
import { messaggioErrore } from '../../shared/utilita/errori';

/** Il pulsante "Scarica PDF" del resoconto del mese scelto nella dashboard */
@Component({
  selector: 'app-pulsante-resoconto',
  template: `
    <button type="button" class="btn btn-outline-secondary btn-sm" [disabled]="inCorso()"
            title="Scarica il resoconto del mese in PDF" (click)="scarica()">
      @if (inCorso()) {
        <span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
      } @else {
        <i class="bi bi-file-earmark-pdf me-1"></i>
      }
      Scarica PDF
    </button>
    @if (errore()) {
      <span class="text-danger small">{{ errore() }}</span>
    }
  `,
})
export class PulsanteResocontoComponent {
  private readonly reportService = inject(ReportService);
  private readonly spaziLavoro = inject(SpazioLavoroService);

  readonly mese = input.required<number>();
  readonly anno = input.required<number>();

  readonly inCorso = signal(false);
  readonly errore = signal<string | null>(null);

  scarica(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null || this.inCorso()) {
      return;
    }
    const mese = this.mese();
    const anno = this.anno();
    this.inCorso.set(true);
    this.errore.set(null);

    this.reportService.scaricaResocontoMensile(spazioId, mese, anno).subscribe({
      next: (pdf) => {
        scaricaFile(pdf, `resoconto-${anno}-${String(mese).padStart(2, '0')}.pdf`);
        this.inCorso.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.inCorso.set(false);
      },
    });
  }
}
