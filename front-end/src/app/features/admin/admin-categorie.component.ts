// src/app/features/admin/admin-categorie.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AdminService } from '../../core/services/admin.service';
import { CategoriaGlobale, CategoriaGlobaleRequest } from '../../models/admin.model';
import { TipoSpazio } from '../../models/spazio-lavoro.model';
import { TipoTransazione } from '../../models/transazione.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { messaggioErrore } from '../../shared/utilita/errori';

const VALORI_INIZIALI = {
  nome: '',
  tipo: 'USCITA' as TipoTransazione,
  colore: '#6c757d',
  descrizione: '',
  tipoSpazio: '' as TipoSpazio | '',
};

@Component({
  selector: 'app-admin-categorie',
  imports: [ReactiveFormsModule, ModaleConfermaComponent],
  template: `
    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body">
        <h2 class="h6 mb-3">{{ inModifica() ? 'Modifica categoria globale' : 'Nuova categoria globale' }}</h2>
        <form [formGroup]="modulo" (ngSubmit)="salva()" novalidate>
          <div class="row g-2 align-items-end">
            <div class="col-12 col-md-3">
              <label class="form-label small mb-1" for="g-nome">Nome</label>
              <input id="g-nome" type="text" class="form-control form-control-sm" formControlName="nome"
                     maxlength="100" [class.is-invalid]="nomeNonValido()" />
              <div class="invalid-feedback">Il nome è obbligatorio.</div>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="g-tipo">Tipo</label>
              <select id="g-tipo" class="form-select form-select-sm" formControlName="tipo">
                <option value="USCITA">Uscita</option>
                <option value="ENTRATA">Entrata</option>
              </select>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="g-visibile">Visibile a</label>
              <select id="g-visibile" class="form-select form-select-sm" formControlName="tipoSpazio">
                <option value="">Tutti</option>
                <option value="PERSONALE">Spazi personali</option>
                <option value="AZIENDALE">Spazi aziendali</option>
              </select>
            </div>
            <div class="col-4 col-md-1">
              <label class="form-label small mb-1" for="g-colore">Colore</label>
              <input id="g-colore" type="color" class="form-control form-control-sm form-control-color w-100"
                     formControlName="colore" />
            </div>
            <div class="col-8 col-md-4">
              <label class="form-label small mb-1" for="g-descrizione">Descrizione</label>
              <input id="g-descrizione" type="text" class="form-control form-control-sm"
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

    <div class="card border-0 shadow-sm">
      <ul class="list-group list-group-flush">
        @for (categoria of categorie(); track categoria.id) {
          <li class="list-group-item d-flex align-items-center gap-3">
            <span class="d-inline-block rounded-circle flex-shrink-0 pallino pallino-medio"
                  [style.--colore]="categoria.colore"></span>
            <div class="flex-grow-1">
              <div class="fw-semibold">{{ categoria.nome }}</div>
              <div class="small text-secondary">Visibile a: {{ etichettaVisibilita(categoria) }}</div>
            </div>
            <span class="badge" [class.text-bg-success]="categoria.tipo === 'ENTRATA'"
                  [class.text-bg-danger]="categoria.tipo === 'USCITA'">
              {{ categoria.tipo === 'ENTRATA' ? 'Entrata' : 'Uscita' }}
            </span>
            @if (eProtetta(categoria)) {
              <span class="badge text-bg-light border" title="Usata dai pagamenti degli stipendi">
                <i class="bi bi-lock me-1"></i>Di sistema
              </span>
            } @else {
              <button type="button" class="btn btn-sm btn-outline-secondary" title="Modifica"
                      (click)="modifica(categoria)"><i class="bi bi-pencil"></i></button>
              <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                      (click)="daEliminare.set(categoria)"><i class="bi bi-trash"></i></button>
            }
          </li>
        }
      </ul>
    </div>

    @if (daEliminare(); as categoria) {
      <app-modale-conferma
        titolo="Elimina categoria globale"
        [messaggio]="'Vuoi eliminare la categoria &quot;' + categoria.nome + '&quot;?'"
        etichettaConferma="Elimina"
        [inCorso]="eliminazioneInCorso()"
        (confermato)="eliminaConfermata(categoria)"
        (annullato)="daEliminare.set(null)"
      />
    }
  `,
})
export class AdminCategorieComponent implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly admin = inject(AdminService);

  readonly modulo = this.fb.group({
    nome: [VALORI_INIZIALI.nome, [Validators.required, Validators.maxLength(100)]],
    tipo: this.fb.control<TipoTransazione>(VALORI_INIZIALI.tipo),
    colore: [VALORI_INIZIALI.colore],
    descrizione: [VALORI_INIZIALI.descrizione, [Validators.maxLength(255)]],
    tipoSpazio: this.fb.control<TipoSpazio | ''>(VALORI_INIZIALI.tipoSpazio),
  });

  readonly categorie = signal<CategoriaGlobale[]>([]);
  readonly errore = signal<string | null>(null);
  readonly salvataggioInCorso = signal(false);
  readonly inModifica = signal<number | null>(null);
  readonly daEliminare = signal<CategoriaGlobale | null>(null);
  readonly eliminazioneInCorso = signal(false);

  ngOnInit(): void {
    this.carica();
  }

  nomeNonValido(): boolean {
    const nome = this.modulo.controls.nome;
    return nome.invalid && (nome.touched || nome.dirty);
  }

  etichettaVisibilita(categoria: CategoriaGlobale): string {
    switch (categoria.tipoSpazio) {
      case 'PERSONALE':
        return 'spazi personali';
      case 'AZIENDALE':
        return 'spazi aziendali';
      default:
        return 'tutti gli spazi';
    }
  }

  /** "Stipendi" è cercata per nome dai pagamenti degli stipendi: non si tocca (il backend la protegge) */
  eProtetta(categoria: CategoriaGlobale): boolean {
    return categoria.nome.toLowerCase() === 'stipendi' && categoria.tipo === 'USCITA';
  }

  modifica(categoria: CategoriaGlobale): void {
    this.inModifica.set(categoria.id);
    this.modulo.reset({
      nome: categoria.nome,
      tipo: categoria.tipo,
      colore: categoria.colore ?? VALORI_INIZIALI.colore,
      descrizione: categoria.descrizione ?? '',
      tipoSpazio: categoria.tipoSpazio ?? '',
    });
  }

  annullaModifica(): void {
    this.inModifica.set(null);
    this.modulo.reset(VALORI_INIZIALI);
  }

  salva(): void {
    if (this.modulo.invalid) {
      this.modulo.markAllAsTouched();
      return;
    }
    const v = this.modulo.getRawValue();
    const richiesta: CategoriaGlobaleRequest = {
      nome: v.nome.trim(),
      tipo: v.tipo,
      colore: v.colore,
      descrizione: v.descrizione.trim() || null,
      tipoSpazio: v.tipoSpazio || null,
    };

    this.salvataggioInCorso.set(true);
    this.errore.set(null);
    const idModifica = this.inModifica();
    const chiamata =
      idModifica === null ? this.admin.creaCategoria(richiesta) : this.admin.aggiornaCategoria(idModifica, richiesta);

    chiamata.subscribe({
      next: () => {
        this.salvataggioInCorso.set(false);
        this.annullaModifica();
        this.carica();
      },
      error: (errore: unknown) => {
        this.salvataggioInCorso.set(false);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  eliminaConfermata(categoria: CategoriaGlobale): void {
    this.eliminazioneInCorso.set(true);
    this.admin.eliminaCategoria(categoria.id).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.carica();
      },
      error: (errore: unknown) => {
        // Ad esempio 409: la categoria è già usata da transazioni o budget
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  private carica(): void {
    this.admin.elencaCategorie().subscribe({
      next: (categorie) => this.categorie.set(categorie),
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }
}
