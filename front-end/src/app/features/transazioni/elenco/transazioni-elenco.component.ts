// src/app/features/transazioni/elenco/transazioni-elenco.component.ts
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { AutenticazioneService } from '../../../core/services/autenticazione.service';
import { CategoriaService } from '../../../core/services/categoria.service';
import { SpazioLavoroService } from '../../../core/services/spazio-lavoro.service';
import { TransazioneService } from '../../../core/services/transazione.service';
import { Categoria } from '../../../models/categoria.model';
import { Pagina } from '../../../models/pagina.model';
import {
  METODI_PAGAMENTO,
  MetodoPagamento,
  RicercaTransazioni,
  TipoTransazione,
  Transazione,
} from '../../../models/transazione.model';
import { ModaleConfermaComponent } from '../../../shared/componenti/modale-conferma/modale-conferma.component';
import { messaggioErrore } from '../../../shared/utilita/errori';

const DIMENSIONE_PAGINA = 10;

type CampoOrdinamento = 'data' | 'importo' | 'descrizione' | 'categoria';

@Component({
  selector: 'app-transazioni-elenco',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, ModaleConfermaComponent],
  template: `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <h1 class="h3 mb-0">Transazioni</h1>
      @if (spaziLavoro.puoScrivere()) {
        <a class="btn btn-primary" routerLink="/transazioni/nuova">
          <i class="bi bi-plus-lg me-1"></i>Nuova transazione
        </a>
      }
    </div>

    <div class="card border-0 shadow-sm mb-3">
      <div class="card-body">
        <form [formGroup]="filtri" (ngSubmit)="applicaFiltri()">
          <div class="row g-2">
            <div class="col-12 col-md-4">
              <label class="form-label small mb-1" for="f-testo">Cerca</label>
              <input id="f-testo" type="search" class="form-control form-control-sm"
                     formControlName="testo" placeholder="Descrizione o note" />
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small mb-1" for="f-tipo">Tipo</label>
              <select id="f-tipo" class="form-select form-select-sm" formControlName="tipo">
                <option value="">Tutti</option>
                <option value="ENTRATA">Entrate</option>
                <option value="USCITA">Uscite</option>
              </select>
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small mb-1" for="f-categoria">Categoria</label>
              <select id="f-categoria" class="form-select form-select-sm" formControlName="categoriaId">
                <option [ngValue]="null">Tutte</option>
                @for (categoria of categorie(); track categoria.id) {
                  <option [ngValue]="categoria.id">{{ categoria.nome }}</option>
                }
              </select>
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small mb-1" for="f-metodo">Metodo di pagamento</label>
              <select id="f-metodo" class="form-select form-select-sm" formControlName="metodoPagamento">
                <option value="">Tutti</option>
                @for (metodo of metodi; track metodo.valore) {
                  <option [value]="metodo.valore">{{ metodo.etichetta }}</option>
                }
              </select>
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small mb-1" for="f-dal">Dal</label>
              <input id="f-dal" type="date" class="form-control form-control-sm" formControlName="dal" />
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small mb-1" for="f-al">Al</label>
              <input id="f-al" type="date" class="form-control form-control-sm" formControlName="al" />
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small mb-1" for="f-min">Importo minimo</label>
              <input id="f-min" type="number" min="0" step="0.01" class="form-control form-control-sm"
                     formControlName="importoMin" />
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small mb-1" for="f-max">Importo massimo</label>
              <input id="f-max" type="number" min="0" step="0.01" class="form-control form-control-sm"
                     formControlName="importoMax" />
            </div>
          </div>
          <div class="d-flex gap-2 mt-3">
            <button type="submit" class="btn btn-primary btn-sm"><i class="bi bi-funnel me-1"></i>Filtra</button>
            <button type="button" class="btn btn-light btn-sm" (click)="azzeraFiltri()">Azzera</button>
          </div>
        </form>
      </div>
    </div>

    @if (errore()) {
      <div class="alert alert-danger">{{ errore() }}</div>
    }

    <div class="card border-0 shadow-sm">
      @if (caricamento() && !risultato()) {
        <div class="card-body text-center text-secondary py-5">
          <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
        </div>
      } @else if (risultato()) {
        @let dati = risultato()!;

        @if (dati.contenuto.length === 0) {
          <div class="card-body text-center text-secondary py-5">Nessuna transazione trovata.</div>
        } @else {
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light">
              <tr>
                <th>
                  <button type="button" class="btn btn-link p-0 text-decoration-none fw-semibold text-dark"
                          (click)="ordina('data')">Data<i [class]="iconaOrdinamento('data')"></i></button>
                </th>
                <th>
                  <button type="button" class="btn btn-link p-0 text-decoration-none fw-semibold text-dark"
                          (click)="ordina('descrizione')">Descrizione<i [class]="iconaOrdinamento('descrizione')"></i></button>
                </th>
                <th>
                  <button type="button" class="btn btn-link p-0 text-decoration-none fw-semibold text-dark"
                          (click)="ordina('categoria')">Categoria<i [class]="iconaOrdinamento('categoria')"></i></button>
                </th>
                <th>Metodo</th>
                <th class="text-end">
                  <button type="button" class="btn btn-link p-0 text-decoration-none fw-semibold text-dark"
                          (click)="ordina('importo')">Importo<i [class]="iconaOrdinamento('importo')"></i></button>
                </th>
                <th></th>
              </tr>
              </thead>
              <tbody>
                @for (transazione of dati.contenuto; track transazione.id) {
                  <tr>
                    <td class="text-nowrap">{{ transazione.data | date: 'dd/MM/yyyy' }}</td>
                    <td>
                      <div>{{ transazione.descrizione }}</div>
                      @if (transazione.note) {
                        <div class="small text-secondary">{{ transazione.note }}</div>
                      }
                    </td>
                    <td class="text-nowrap">
                  <span class="d-inline-block rounded-circle me-1 pallino"
                        [style.--colore]="transazione.categoriaColore"></span>
                      {{ transazione.categoriaNome }}
                    </td>
                    <td>{{ etichettaMetodo(transazione.metodoPagamento) }}</td>
                    <td class="text-end text-nowrap fw-semibold"
                        [class.text-success]="transazione.tipo === 'ENTRATA'"
                        [class.text-danger]="transazione.tipo === 'USCITA'">
                      {{ transazione.tipo === 'USCITA' ? '−' : '+' }}{{ transazione.importo | currency: valuta() }}
                    </td>
                    <td class="text-end text-nowrap">
                      @if (puoModificare(transazione)) {
                        <a class="btn btn-sm btn-outline-secondary me-1" title="Modifica"
                           [routerLink]="['/transazioni', transazione.id, 'modifica']">
                          <i class="bi bi-pencil"></i>
                        </a>
                        <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                                (click)="daEliminare.set(transazione)">
                          <i class="bi bi-trash"></i>
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Footer con paginazione -->
          <div class="card-footer bg-white d-flex flex-wrap justify-content-between align-items-center gap-2">
        <span class="small text-secondary">
          Pagina {{ dati.pagina + 1 }} di {{ dati.totalePagine }} · {{ dati.totaleElementi }} risultati
        </span>
            <div class="btn-group">
              <button type="button" class="btn btn-outline-secondary btn-sm"
                      [disabled]="dati.pagina === 0" (click)="vaiAPagina(dati.pagina - 1)">
                <i class="bi bi-chevron-left"></i> Precedente
              </button>
              <button type="button" class="btn btn-outline-secondary btn-sm"
                      [disabled]="dati.pagina + 1 >= dati.totalePagine" (click)="vaiAPagina(dati.pagina + 1)">
                Successiva <i class="bi bi-chevron-right"></i>
              </button>
            </div>
          </div>
        }
      }
    </div>

    @if (daEliminare(); as transazione) {
      <app-modale-conferma
        titolo="Elimina transazione"
        [messaggio]="messaggioEliminazione(transazione)"
        etichettaConferma="Elimina"
        [inCorso]="eliminazioneInCorso()"
        (confermato)="eliminaConfermata(transazione)"
        (annullato)="daEliminare.set(null)"
      />
    }
  `,
})
export class TransazioniElencoComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly transazioneService = inject(TransazioneService);
  private readonly categoriaService = inject(CategoriaService);
  readonly auth = inject(AutenticazioneService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly metodi = METODI_PAGAMENTO;

  readonly filtri = this.fb.group({
    testo: [''],
    tipo: this.fb.control<TipoTransazione | ''>(''),
    categoriaId: this.fb.control<number | null>(null),
    metodoPagamento: this.fb.control<MetodoPagamento | ''>(''),
    dal: [''],
    al: [''],
    importoMin: this.fb.control<number | null>(null),
    importoMax: this.fb.control<number | null>(null),
  });

  readonly categorie = signal<Categoria[]>([]);
  readonly risultato = signal<Pagina<Transazione> | null>(null);
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);

  private readonly paginaCorrente = signal(0);
  private readonly ordinaPer = signal<CampoOrdinamento>('data');
  private readonly direzione = signal<'asc' | 'desc'>('desc');

  readonly daEliminare = signal<Transazione | null>(null);
  readonly eliminazioneInCorso = signal(false);

  readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');

  private richiestaInCorso?: Subscription;

  constructor() {
    // Ogni volta che cambia lo spazio attivo si riparte da zero: filtri, categorie e primo elenco
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        this.filtri.reset();
        this.paginaCorrente.set(0);
        this.caricaCategorie(spazioId);
        this.carica();
      });
    });
  }

  applicaFiltri(): void {
    this.paginaCorrente.set(0);
    this.carica();
  }

  azzeraFiltri(): void {
    this.filtri.reset();
    this.applicaFiltri();
  }

  vaiAPagina(pagina: number): void {
    this.paginaCorrente.set(pagina);
    this.carica();
  }

  ordina(campo: CampoOrdinamento): void {
    if (this.ordinaPer() === campo) {
      this.direzione.update((attuale) => (attuale === 'asc' ? 'desc' : 'asc'));
    } else {
      this.ordinaPer.set(campo);
      this.direzione.set(campo === 'data' || campo === 'importo' ? 'desc' : 'asc');
    }
    this.paginaCorrente.set(0);
    this.carica();
  }

  iconaOrdinamento(campo: CampoOrdinamento): string {
    if (this.ordinaPer() !== campo) {
      return '';
    }
    return this.direzione() === 'asc' ? 'bi bi-caret-up-fill ms-1' : 'bi bi-caret-down-fill ms-1';
  }

  etichettaMetodo(metodo: MetodoPagamento): string {
    return this.metodi.find((m) => m.valore === metodo)?.etichetta ?? metodo;
  }

  /** Chi gestisce lo spazio modifica tutto; un membro solo le transazioni inserite da lui */
  puoModificare(transazione: Transazione): boolean {
    if (this.spaziLavoro.puoGestire()) {
      return true;
    }
    return (
      this.spaziLavoro.puoScrivere() &&
      transazione.creataDaId !== null &&
      transazione.creataDaId === this.auth.utente()?.id
    );
  }

  messaggioEliminazione(transazione: Transazione): string {
    return `Vuoi eliminare "${transazione.descrizione}"? L'operazione non si può annullare.`;
  }

  eliminaConfermata(transazione: Transazione): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.eliminazioneInCorso.set(true);
    this.transazioneService.elimina(spazioId, transazione.id).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.carica();
      },
      error: (errore: unknown) => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  private caricaCategorie(spazioId: number): void {
    this.categoriaService.elenca(spazioId).subscribe({
      next: (categorie) => this.categorie.set(categorie),
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }

  private carica(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    // Se parte una nuova ricerca, quella precedente non serve più
    this.richiestaInCorso?.unsubscribe();
    this.caricamento.set(true);
    this.errore.set(null);

    const v = this.filtri.getRawValue();
    const ricerca: RicercaTransazioni = {
      testo: v.testo.trim() || undefined,
      tipo: v.tipo || undefined,
      categoriaId: v.categoriaId ?? undefined,
      metodoPagamento: v.metodoPagamento || undefined,
      dal: v.dal || undefined,
      al: v.al || undefined,
      importoMin: v.importoMin ?? undefined,
      importoMax: v.importoMax ?? undefined,
      pagina: this.paginaCorrente(),
      dimensione: DIMENSIONE_PAGINA,
      ordinaPer: this.ordinaPer(),
      direzione: this.direzione(),
    };

    this.richiestaInCorso = this.transazioneService.cerca(spazioId, ricerca).subscribe({
      next: (pagina) => {
        this.risultato.set(pagina);
        this.caricamento.set(false);
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.caricamento.set(false);
      },
    });
  }
}
