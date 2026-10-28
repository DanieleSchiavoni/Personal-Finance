// src/app/features/categorie/categorie.component.ts
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaService } from '../../core/services/categoria.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { Categoria, CategoriaRequest } from '../../models/categoria.model';
import { TipoTransazione } from '../../models/transazione.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { messaggioErrore } from '../../shared/utilita/errori';

const VALORI_INIZIALI = { nome: '', tipo: 'USCITA' as TipoTransazione, colore: '#6c757d', descrizione: '' };

@Component({
  selector: 'app-categorie',
  imports: [ReactiveFormsModule, ModaleConfermaComponent],
  template: `
    <h1 class="h3 mb-3">Categorie</h1>

    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    @if (spaziLavoro.puoGestire()) {
      <div class="card border-0 shadow-sm mb-4">
        <div class="card-body">
          <h2 class="h6 mb-3">{{ inModifica() ? 'Modifica categoria' : 'Nuova categoria' }}</h2>
          <form [formGroup]="modulo" (ngSubmit)="salva()" novalidate>
            <div class="row g-2 align-items-end">
              <div class="col-12 col-md-4">
                <label class="form-label small mb-1" for="c-nome">Nome</label>
                <input id="c-nome" type="text" class="form-control form-control-sm" formControlName="nome"
                       maxlength="100" [class.is-invalid]="nomeNonValido()" />
                <div class="invalid-feedback">Il nome è obbligatorio.</div>
              </div>
              <div class="col-6 col-md-2">
                <label class="form-label small mb-1" for="c-tipo">Tipo</label>
                <select id="c-tipo" class="form-select form-select-sm" formControlName="tipo">
                  <option value="USCITA">Uscita</option>
                  <option value="ENTRATA">Entrata</option>
                </select>
              </div>
              <div class="col-6 col-md-2">
                <label class="form-label small mb-1" for="c-colore">Colore</label>
                <input id="c-colore" type="color" class="form-control form-control-sm form-control-color w-100"
                       formControlName="colore" />
              </div>
              <div class="col-12 col-md-4">
                <label class="form-label small mb-1" for="c-descrizione">Descrizione</label>
                <input id="c-descrizione" type="text" class="form-control form-control-sm"
                       formControlName="descrizione" maxlength="255" />
              </div>
            </div>
            <div class="d-flex gap-2 mt-3">
              <button type="submit" class="btn btn-primary btn-sm" [disabled]="salvataggioInCorso()">
                {{ inModifica() ? 'Salva modifiche' : 'Aggiungi categoria' }}
              </button>
              @if (inModifica()) {
                <button type="button" class="btn btn-light btn-sm" (click)="annullaModifica()">Annulla</button>
              }
            </div>
          </form>
        </div>
      </div>
    }

    <div class="card border-0 shadow-sm mb-4">
      <div class="card-header bg-white fw-semibold">Categorie di questo spazio</div>
      @if (proprie().length === 0) {
        <div class="card-body text-secondary">Non hai ancora creato categorie personalizzate.</div>
      } @else {
        <ul class="list-group list-group-flush">
          @for (categoria of proprie(); track categoria.id) {
            <li class="list-group-item d-flex align-items-center gap-3">
              <span class="d-inline-block rounded-circle flex-shrink-0 pallino pallino-medio"
                    [style.--colore]="categoria.colore"></span>
              <div class="flex-grow-1">
                <div class="fw-semibold">{{ categoria.nome }}</div>
                @if (categoria.descrizione) {
                  <div class="small text-secondary">{{ categoria.descrizione }}</div>
                }
              </div>
              <span class="badge" [class.text-bg-success]="categoria.tipo === 'ENTRATA'"
                    [class.text-bg-danger]="categoria.tipo === 'USCITA'">
                {{ categoria.tipo === 'ENTRATA' ? 'Entrata' : 'Uscita' }}
              </span>
              @if (spaziLavoro.puoGestire()) {
                <button type="button" class="btn btn-sm btn-outline-secondary" title="Modifica"
                        (click)="modifica(categoria)"><i class="bi bi-pencil"></i></button>
                <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                        (click)="daEliminare.set(categoria)"><i class="bi bi-trash"></i></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    <div class="card border-0 shadow-sm">
      <div class="card-header bg-white fw-semibold">Categorie predefinite</div>
      <ul class="list-group list-group-flush">
        @for (categoria of predefinite(); track categoria.id) {
          <li class="list-group-item d-flex align-items-center gap-3">
            <span class="d-inline-block rounded-circle flex-shrink-0 pallino pallino-medio"
                  [style.--colore]="categoria.colore"></span>
            <div class="flex-grow-1">{{ categoria.nome }}</div>
            <span class="badge" [class.text-bg-success]="categoria.tipo === 'ENTRATA'"
                  [class.text-bg-danger]="categoria.tipo === 'USCITA'">
              {{ categoria.tipo === 'ENTRATA' ? 'Entrata' : 'Uscita' }}
            </span>
          </li>
        }
      </ul>
    </div>

    @if (daEliminare(); as categoria) {
      <app-modale-conferma
        titolo="Elimina categoria"
        [messaggio]="'Vuoi eliminare la categoria &quot;' + categoria.nome + '&quot;?'"
        etichettaConferma="Elimina"
        [inCorso]="eliminazioneInCorso()"
        (confermato)="eliminaConfermata(categoria)"
        (annullato)="daEliminare.set(null)"
      />
    }
  `,
})
export class CategorieComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly categoriaService = inject(CategoriaService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly modulo = this.fb.group({
    nome: [VALORI_INIZIALI.nome, [Validators.required, Validators.maxLength(100)]],
    tipo: this.fb.control<TipoTransazione>(VALORI_INIZIALI.tipo),
    colore: [VALORI_INIZIALI.colore],
    descrizione: [VALORI_INIZIALI.descrizione, [Validators.maxLength(255)]],
  });

  readonly categorie = signal<Categoria[]>([]);
  readonly proprie = computed(() => this.categorie().filter((c) => !c.globale));
  readonly predefinite = computed(() => this.categorie().filter((c) => c.globale));

  readonly errore = signal<string | null>(null);
  readonly salvataggioInCorso = signal(false);
  readonly inModifica = signal<number | null>(null);
  readonly daEliminare = signal<Categoria | null>(null);
  readonly eliminazioneInCorso = signal(false);

  constructor() {
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        this.annullaModifica();
        this.carica(spazioId);
      });
    });
  }

  nomeNonValido(): boolean {
    const nome = this.modulo.controls.nome;
    return nome.invalid && (nome.touched || nome.dirty);
  }

  modifica(categoria: Categoria): void {
    this.inModifica.set(categoria.id);
    this.modulo.reset({
      nome: categoria.nome,
      tipo: categoria.tipo,
      colore: categoria.colore ?? VALORI_INIZIALI.colore,
      descrizione: categoria.descrizione ?? '',
    });
  }

  annullaModifica(): void {
    this.inModifica.set(null);
    this.modulo.reset(VALORI_INIZIALI);
  }

  salva(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (this.modulo.invalid || spazioId === null) {
      this.modulo.markAllAsTouched();
      return;
    }
    const v = this.modulo.getRawValue();
    const richiesta: CategoriaRequest = {
      nome: v.nome.trim(),
      tipo: v.tipo,
      colore: v.colore,
      descrizione: v.descrizione.trim() || null,
    };

    this.salvataggioInCorso.set(true);
    this.errore.set(null);
    const idModifica = this.inModifica();
    const chiamata =
      idModifica === null
        ? this.categoriaService.crea(spazioId, richiesta)
        : this.categoriaService.aggiorna(spazioId, idModifica, richiesta);

    chiamata.subscribe({
      next: () => {
        this.salvataggioInCorso.set(false);
        this.annullaModifica();
        this.carica(spazioId);
      },
      error: (errore: unknown) => {
        this.salvataggioInCorso.set(false);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  eliminaConfermata(categoria: Categoria): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.eliminazioneInCorso.set(true);
    this.categoriaService.elimina(spazioId, categoria.id).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.carica(spazioId);
      },
      error: (errore: unknown) => {
        // Ad esempio 409: la categoria è già usata da transazioni o budget
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  private carica(spazioId: number): void {
    this.errore.set(null);
    this.categoriaService.elenca(spazioId).subscribe({
      next: (categorie) => this.categorie.set(categorie),
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }
}
