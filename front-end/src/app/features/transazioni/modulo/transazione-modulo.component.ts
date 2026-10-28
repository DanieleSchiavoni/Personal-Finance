// src/app/features/transazioni/modulo/transazione-modulo.component.ts
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CategoriaService } from '../../../core/services/categoria.service';
import { SpazioLavoroService } from '../../../core/services/spazio-lavoro.service';
import { TransazioneService } from '../../../core/services/transazione.service';
import { Categoria } from '../../../models/categoria.model';
import {
  METODI_PAGAMENTO,
  MetodoPagamento,
  TipoTransazione,
  TransazioneRequest,
} from '../../../models/transazione.model';
import { oggiIso } from '../../../shared/utilita/date';
import { messaggioErrore } from '../../../shared/utilita/errori';

type Campo = 'importo' | 'descrizione' | 'data' | 'categoriaId' | 'note';

/** Serve sia per inserire (/transazioni/nuova) sia per modificare (/transazioni/:id/modifica) */
@Component({
  selector: 'app-transazione-modulo',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="d-flex align-items-center gap-2 mb-3">
      <a routerLink="/transazioni" class="btn btn-light btn-sm" aria-label="Torna all'elenco">
        <i class="bi bi-arrow-left"></i>
      </a>
      <h1 class="h3 mb-0">{{ inModifica ? 'Modifica transazione' : 'Nuova transazione' }}</h1>
    </div>

    @if (!spaziLavoro.puoScrivere()) {
      <div class="alert alert-warning">In questo spazio hai solo la possibilità di consultare i dati.</div>
    } @else {
      <div class="card border-0 shadow-sm larghezza-max-640">
        <div class="card-body p-4">
          @if (errore()) {
            <div class="alert alert-danger py-2" role="alert">{{ errore() }}</div>
          }

          <form [formGroup]="modulo" (ngSubmit)="salva()" novalidate>
            <div class="btn-group w-100 mb-3" role="group" aria-label="Tipo di transazione">
              <input type="radio" class="btn-check" id="tipo-uscita" formControlName="tipo" value="USCITA" autocomplete="off" />
              <label class="btn btn-outline-danger" for="tipo-uscita">Uscita</label>
              <input type="radio" class="btn-check" id="tipo-entrata" formControlName="tipo" value="ENTRATA" autocomplete="off" />
              <label class="btn btn-outline-success" for="tipo-entrata">Entrata</label>
            </div>

            <div class="row g-3">
              <div class="col-sm-6">
                <label for="importo" class="form-label">Importo ({{ spaziLavoro.spazioAttivo()?.valuta }})</label>
                <input id="importo" type="number" min="0.01" step="0.01" class="form-control"
                       formControlName="importo" [class.is-invalid]="nonValido('importo')" />
                <div class="invalid-feedback">Inserisci un importo maggiore di zero.</div>
              </div>
              <div class="col-sm-6">
                <label for="data" class="form-label">Data</label>
                <input id="data" type="date" class="form-control" formControlName="data"
                       [class.is-invalid]="nonValido('data')" />
                <div class="invalid-feedback">La data è obbligatoria.</div>
              </div>
              <div class="col-12">
                <label for="descrizione" class="form-label">Descrizione</label>
                <input id="descrizione" type="text" class="form-control" formControlName="descrizione"
                       maxlength="255" [class.is-invalid]="nonValido('descrizione')" />
                <div class="invalid-feedback">La descrizione è obbligatoria (massimo 255 caratteri).</div>
              </div>
              <div class="col-sm-6">
                <label for="categoria" class="form-label">Categoria</label>
                <select id="categoria" class="form-select" formControlName="categoriaId"
                        [class.is-invalid]="nonValido('categoriaId')">
                  <option [ngValue]="null" disabled>Scegli una categoria</option>
                  @for (categoria of categorieDelTipo(); track categoria.id) {
                    <option [ngValue]="categoria.id">{{ categoria.nome }}</option>
                  }
                </select>
                <div class="invalid-feedback">Scegli una categoria.</div>
              </div>
              <div class="col-sm-6">
                <label for="metodo" class="form-label">Metodo di pagamento</label>
                <select id="metodo" class="form-select" formControlName="metodoPagamento">
                  @for (metodo of metodi; track metodo.valore) {
                    <option [value]="metodo.valore">{{ metodo.etichetta }}</option>
                  }
                </select>
              </div>
              <div class="col-12">
                <label for="note" class="form-label">Note (facoltative)</label>
                <textarea id="note" rows="2" class="form-control" formControlName="note" maxlength="500"
                          [class.is-invalid]="nonValido('note')"></textarea>
                <div class="invalid-feedback">Le note possono avere al massimo 500 caratteri.</div>
              </div>
            </div>

            <div class="d-flex gap-2 mt-4">
              <button type="submit" class="btn btn-primary" [disabled]="inCorso()">
                @if (inCorso()) {
                  <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
                }
                {{ inModifica ? 'Salva modifiche' : 'Aggiungi' }}
              </button>
              <a routerLink="/transazioni" class="btn btn-light">Annulla</a>
            </div>
          </form>
        </div>
      </div>
    }
  `,
})
export class TransazioneModuloComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly transazioneService = inject(TransazioneService);
  private readonly categoriaService = inject(CategoriaService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly metodi = METODI_PAGAMENTO;

  /** L'id nell'indirizzo c'è solo in modifica */
  private readonly transazioneId = Number(this.route.snapshot.paramMap.get('id')) || null;
  readonly inModifica = this.transazioneId !== null;

  readonly modulo = this.fb.group({
    tipo: this.fb.control<TipoTransazione>('USCITA'),
    importo: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    descrizione: ['', [Validators.required, Validators.maxLength(255)]],
    data: [oggiIso(), [Validators.required]],
    categoriaId: this.fb.control<number | null>(null, [Validators.required]),
    metodoPagamento: this.fb.control<MetodoPagamento>('CARTA'),
    note: ['', [Validators.maxLength(500)]],
  });

  readonly categorie = signal<Categoria[]>([]);
  private readonly tipo = toSignal(this.modulo.controls.tipo.valueChanges, {
    initialValue: this.modulo.controls.tipo.value,
  });
  /** Le categorie proposte dipendono dal tipo scelto: entrata o uscita */
  readonly categorieDelTipo = computed(() => this.categorie().filter((c) => c.tipo === this.tipo()));

  readonly inCorso = signal(false);
  readonly errore = signal<string | null>(null);

  constructor() {
    // Se si cambia il tipo e la categoria scelta non è più adatta, la si azzera
    this.modulo.controls.tipo.valueChanges.pipe(takeUntilDestroyed()).subscribe((tipo) => {
      const scelta = this.categorie().find((c) => c.id === this.modulo.controls.categoriaId.value);
      if (scelta && scelta.tipo !== tipo) {
        this.modulo.controls.categoriaId.reset(null);
      }
    });

    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => this.caricaDati(spazioId));
    });
  }

  salva(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const v = this.modulo.getRawValue();
    if (this.modulo.invalid || spazioId === null || v.importo === null || v.categoriaId === null) {
      this.modulo.markAllAsTouched();
      return;
    }
    const richiesta: TransazioneRequest = {
      importo: v.importo,
      descrizione: v.descrizione.trim(),
      data: v.data,
      tipo: v.tipo,
      metodoPagamento: v.metodoPagamento,
      categoriaId: v.categoriaId,
      note: v.note.trim() || null,
    };

    this.inCorso.set(true);
    this.errore.set(null);
    const chiamata =
      this.transazioneId === null
        ? this.transazioneService.crea(spazioId, richiesta)
        : this.transazioneService.aggiorna(spazioId, this.transazioneId, richiesta);

    chiamata.subscribe({
      next: () => this.router.navigate(['/transazioni']),
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.inCorso.set(false);
      },
    });
  }

  nonValido(campo: Campo): boolean {
    const controllo = this.modulo.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  private caricaDati(spazioId: number): void {
    this.categoriaService.elenca(spazioId).subscribe({
      next: (categorie) => this.categorie.set(categorie),
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });

    if (this.transazioneId !== null) {
      this.transazioneService.dettaglio(spazioId, this.transazioneId).subscribe({
        next: (t) =>
          this.modulo.patchValue({
            tipo: t.tipo,
            importo: t.importo,
            descrizione: t.descrizione,
            data: t.data,
            categoriaId: t.categoriaId,
            metodoPagamento: t.metodoPagamento,
            note: t.note ?? '',
          }),
        error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
      });
    }
  }
}
