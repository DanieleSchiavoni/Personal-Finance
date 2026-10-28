// src/app/features/dipendenti/dipendenti.component.ts
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, forkJoin, map, of, switchMap } from 'rxjs';

import { DipendenteService } from '../../core/services/dipendente.service';
import { SpazioLavoroService } from '../../core/services/spazio-lavoro.service';
import { Dipendente, DipendenteRequest, PagamentoStipendio } from '../../models/dipendente.model';
import { METODI_PAGAMENTO, MetodoPagamento } from '../../models/transazione.model';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { SchedaKpiComponent } from '../../shared/componenti/scheda-kpi/scheda-kpi.component';
import { SelettoreMeseComponent } from '../../shared/componenti/selettore-mese/selettore-mese.component';
import { oggiIso } from '../../shared/utilita/date';
import { messaggioErrore } from '../../shared/utilita/errori';

@Component({
  selector: 'app-dipendenti',
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    DatePipe,
    SelettoreMeseComponent,
    SchedaKpiComponent,
    ModaleConfermaComponent,
  ],
  template: `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <div>
        <h1 class="h3 mb-0">Dipendenti</h1>
        @if (spaziLavoro.spazioAttivo(); as spazio) {
          <div class="text-secondary small">{{ spazio.nome }}</div>
        }
      </div>
      @if (visibile()) {
        <app-selettore-mese [(mese)]="mese" [(anno)]="anno" />
      }
    </div>

    @if (!eAziendale()) {
      <div class="alert alert-info">I dipendenti sono disponibili solo negli spazi aziendali.</div>
    } @else if (!spaziLavoro.puoGestire()) {
      <div class="alert alert-warning">
        I dati dei dipendenti sono riservati al proprietario e agli amministratori dello spazio.
      </div>
    } @else {
      @if (errore()) {
        <div class="alert alert-danger">{{ errore() }}</div>
      }
      @if (messaggio()) {
        <div class="alert alert-success">{{ messaggio() }}</div>
      }

      <div class="row g-3 mb-3">
        <div class="col-12 col-md-4">
          <app-scheda-kpi titolo="Costo mensile del personale" icona="bi-people"
                          nota="Stipendi dei dipendenti attivi">
            {{ costoMensile() | currency: valuta() }}
          </app-scheda-kpi>
        </div>
        <div class="col-6 col-md-4">
          <app-scheda-kpi titolo="Pagato nel mese" icona="bi-check2-circle" classeValore="text-success">
            {{ pagatoNelMese() | currency: valuta() }}
          </app-scheda-kpi>
        </div>
        <div class="col-6 col-md-4">
          <app-scheda-kpi titolo="Ancora da pagare" icona="bi-hourglass-split"
                          [classeValore]="daPagare() > 0 ? 'text-danger' : 'text-success'">
            {{ daPagare() | currency: valuta() }}
          </app-scheda-kpi>
        </div>
      </div>

      <div class="card border-0 shadow-sm mb-4">
        <div class="card-body">
          <h2 class="h6 mb-3">{{ inModifica() !== null ? 'Modifica dipendente' : 'Nuovo dipendente' }}</h2>
          <form [formGroup]="modulo" (ngSubmit)="salva()" novalidate>
            <div class="row g-2 align-items-end">
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="d-nome">Nome</label>
                <input id="d-nome" type="text" class="form-control form-control-sm" formControlName="nome"
                       maxlength="100" [class.is-invalid]="nonValido('nome')" />
                <div class="invalid-feedback">Il nome è obbligatorio.</div>
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="d-cognome">Cognome</label>
                <input id="d-cognome" type="text" class="form-control form-control-sm" formControlName="cognome"
                       maxlength="100" [class.is-invalid]="nonValido('cognome')" />
                <div class="invalid-feedback">Il cognome è obbligatorio.</div>
              </div>
              <div class="col-12 col-md-3">
                <label class="form-label small mb-1" for="d-mansione">Mansione</label>
                <input id="d-mansione" type="text" class="form-control form-control-sm" formControlName="mansione"
                       maxlength="100" />
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="d-stipendio">Stipendio mensile ({{ valuta() }})</label>
                <input id="d-stipendio" type="number" min="0" step="0.01" class="form-control form-control-sm"
                       formControlName="stipendioMensile" [class.is-invalid]="nonValido('stipendioMensile')" />
                <div class="invalid-feedback">Inserisci lo stipendio mensile.</div>
              </div>
              <div class="col-6 col-md-3">
                <label class="form-label small mb-1" for="d-assunzione">Data di assunzione</label>
                <input id="d-assunzione" type="date" class="form-control form-control-sm"
                       formControlName="dataAssunzione" />
              </div>
              <div class="col-6 col-md-3">
                <div class="form-check mb-1">
                  <input id="d-attivo" type="checkbox" class="form-check-input" formControlName="attivo" />
                  <label class="form-check-label small" for="d-attivo">Dipendente attivo</label>
                </div>
              </div>
              <div class="col-12 col-md-6 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm" [disabled]="salvataggioInCorso()">
                  {{ inModifica() !== null ? 'Salva modifiche' : 'Aggiungi dipendente' }}
                </button>
                @if (inModifica() !== null) {
                  <button type="button" class="btn btn-light btn-sm" (click)="annullaModifica()">Annulla</button>
                }
              </div>
            </div>
          </form>
        </div>
      </div>

      <div class="card border-0 shadow-sm">
        @if (caricamento() && dipendenti().length === 0) {
          <div class="card-body text-center text-secondary py-5">
            <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Caricamento…
          </div>
        } @else if (dipendenti().length === 0) {
          <div class="card-body text-center text-secondary py-5">
            <i class="bi bi-people fs-3 d-block mb-2"></i>Non hai ancora aggiunto dipendenti.
          </div>
        } @else {
          <div class="table-responsive" [class.opacity-50]="caricamento()">
            <table class="table align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th>Dipendente</th>
                  <th class="text-end">Stipendio mensile</th>
                  <th>Stato</th>
                  <th class="text-capitalize">Stipendio di {{ dataSelezionata() | date: 'LLLL y' }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (dipendente of dipendenti(); track dipendente.id) {
                  <tr>
                    <td>
                      <div class="fw-semibold">{{ dipendente.nome }} {{ dipendente.cognome }}</div>
                      <div class="small text-secondary">
                        {{ dipendente.mansione ?? 'Mansione non indicata' }}
                        @if (dipendente.dataAssunzione) {
                          · dal {{ dipendente.dataAssunzione | date: 'dd/MM/yyyy' }}
                        }
                      </div>
                    </td>
                    <td class="text-end text-nowrap">{{ dipendente.stipendioMensile | currency: valuta() }}</td>
                    <td>
                      <span class="badge" [class.text-bg-success]="dipendente.attivo"
                            [class.text-bg-secondary]="!dipendente.attivo">
                        {{ dipendente.attivo ? 'Attivo' : 'Non attivo' }}
                      </span>
                    </td>
                    <td>
                      @if (pagamentoDelMese(dipendente); as pagamento) {
                        <span class="badge text-bg-success"><i class="bi bi-check2 me-1"></i>Pagato</span>
                        <div class="small text-secondary">
                          {{ pagamento.importo | currency: valuta() }} · {{ pagamento.dataPagamento | date: 'dd/MM/yyyy' }}
                        </div>
                      } @else if (dipendente.attivo) {
                        <button type="button" class="btn btn-sm btn-primary" (click)="apriPagamento(dipendente)">
                          Registra stipendio
                        </button>
                      } @else {
                        <span class="text-secondary small">—</span>
                      }
                    </td>
                    <td class="text-end text-nowrap">
                      <button type="button" class="btn btn-sm btn-outline-secondary me-1" title="Storico dei pagamenti"
                              (click)="alternaStorico(dipendente)">
                        <i class="bi" [class.bi-chevron-down]="espanso() !== dipendente.id"
                           [class.bi-chevron-up]="espanso() === dipendente.id"></i>
                      </button>
                      <button type="button" class="btn btn-sm btn-outline-secondary me-1" title="Modifica"
                              (click)="modifica(dipendente)"><i class="bi bi-pencil"></i></button>
                      <button type="button" class="btn btn-sm btn-outline-secondary me-1"
                              [title]="dipendente.attivo ? 'Disattiva' : 'Riattiva'" (click)="cambiaStato(dipendente)">
                        <i class="bi" [class.bi-person-dash]="dipendente.attivo"
                           [class.bi-person-check]="!dipendente.attivo"></i>
                      </button>
                      <button type="button" class="btn btn-sm btn-outline-danger" title="Elimina"
                              (click)="daEliminare.set(dipendente)"><i class="bi bi-trash"></i></button>
                    </td>
                  </tr>
                  @if (espanso() === dipendente.id) {
                    <tr class="table-light">
                      <td colspan="5">
                        @if ((pagamenti()[dipendente.id] ?? []).length === 0) {
                          <span class="small text-secondary">Nessun pagamento registrato.</span>
                        } @else {
                          <table class="table table-sm mb-0">
                            <thead>
                              <tr><th>Mese</th><th class="text-end">Importo</th><th>Pagato il</th></tr>
                            </thead>
                            <tbody>
                              @for (pagamento of pagamenti()[dipendente.id]; track pagamento.transazioneId) {
                                <tr>
                                  <td class="text-capitalize">{{ dataMese(pagamento.mese, pagamento.anno) | date: 'LLLL y' }}</td>
                                  <td class="text-end">{{ pagamento.importo | currency: valuta() }}</td>
                                  <td>{{ pagamento.dataPagamento | date: 'dd/MM/yyyy' }}</td>
                                </tr>
                              }
                            </tbody>
                          </table>
                        }
                      </td>
                    </tr>
                  }
                }
              </tbody>
            </table>
          </div>
        }
      </div>
      <p class="small text-secondary mt-3">
        Ogni stipendio registrato diventa una transazione di uscita nella categoria "Stipendi". I singoli pagamenti
        sono visibili solo a chi gestisce lo spazio; i totali compaiono invece nella dashboard.
      </p>
    }

    @if (daPagare$(); as dipendente) {
      <app-modale-conferma
        [titolo]="'Stipendio di ' + dipendente.nome + ' ' + dipendente.cognome"
        [messaggio]="messaggioPagamento()"
        etichettaConferma="Registra"
        stile="primary"
        [inCorso]="pagamentoInCorso()"
        [confermaDisabilitata]="pagamento.invalid"
        (confermato)="registraPagamento(dipendente)"
        (annullato)="daPagare$.set(null)"
      >
        <div [formGroup]="pagamento" class="mt-3">
          <div class="row g-2">
            <div class="col-6">
              <label class="form-label small mb-1" for="p-importo">Importo ({{ valuta() }})</label>
              <input id="p-importo" type="number" min="0.01" step="0.01" class="form-control form-control-sm"
                     formControlName="importo" />
            </div>
            <div class="col-6">
              <label class="form-label small mb-1" for="p-data">Data del pagamento</label>
              <input id="p-data" type="date" class="form-control form-control-sm" formControlName="dataPagamento" />
            </div>
            <div class="col-12">
              <label class="form-label small mb-1" for="p-metodo">Metodo di pagamento</label>
              <select id="p-metodo" class="form-select form-select-sm" formControlName="metodoPagamento">
                @for (metodo of metodi; track metodo.valore) {
                  <option [value]="metodo.valore">{{ metodo.etichetta }}</option>
                }
              </select>
            </div>
          </div>
          @if (errorePagamento()) {
            <div class="text-danger small mt-2">{{ errorePagamento() }}</div>
          }
        </div>
      </app-modale-conferma>
    }

    @if (daEliminare(); as dipendente) {
      <app-modale-conferma
        titolo="Elimina dipendente"
        [messaggio]="'Vuoi eliminare ' + dipendente.nome + ' ' + dipendente.cognome + '? Se ha già pagamenti registrati non è possibile: in quel caso disattivalo.'"
        etichettaConferma="Elimina"
        [inCorso]="eliminazioneInCorso()"
        (confermato)="eliminaConfermata(dipendente)"
        (annullato)="daEliminare.set(null)"
      />
    }
  `,
})
export class DipendentiComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly dipendenteService = inject(DipendenteService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly metodi = METODI_PAGAMENTO;

  private readonly oggi = new Date();
  readonly mese = signal(this.oggi.getMonth() + 1);
  readonly anno = signal(this.oggi.getFullYear());

  readonly dipendenti = signal<Dipendente[]>([]);
  /** I pagamenti di ogni dipendente, per id del dipendente */
  readonly pagamenti = signal<Record<number, PagamentoStipendio[]>>({});
  readonly caricamento = signal(false);
  readonly errore = signal<string | null>(null);
  readonly messaggio = signal<string | null>(null);

  readonly modulo = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    cognome: ['', [Validators.required, Validators.maxLength(100)]],
    mansione: ['', [Validators.maxLength(100)]],
    stipendioMensile: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    dataAssunzione: [''],
    attivo: [true],
  });
  readonly inModifica = signal<number | null>(null);
  readonly salvataggioInCorso = signal(false);

  readonly espanso = signal<number | null>(null);

  readonly daPagare$ = signal<Dipendente | null>(null);
  readonly pagamento = this.fb.group({
    importo: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    dataPagamento: [oggiIso(), [Validators.required]],
    metodoPagamento: this.fb.control<MetodoPagamento>('BONIFICO'),
  });
  readonly pagamentoInCorso = signal(false);
  readonly errorePagamento = signal<string | null>(null);

  readonly daEliminare = signal<Dipendente | null>(null);
  readonly eliminazioneInCorso = signal(false);

  readonly valuta = computed(() => this.spaziLavoro.spazioAttivo()?.valuta ?? 'EUR');
  readonly eAziendale = computed(() => this.spaziLavoro.spazioAttivo()?.tipo === 'AZIENDALE');
  readonly visibile = computed(() => this.eAziendale() && this.spaziLavoro.puoGestire());
  readonly dataSelezionata = computed(() => new Date(this.anno(), this.mese() - 1, 1));

  readonly costoMensile = computed(() =>
    this.dipendenti()
      .filter((dipendente) => dipendente.attivo)
      .reduce((somma, dipendente) => somma + dipendente.stipendioMensile, 0),
  );

  readonly pagatoNelMese = computed(() =>
    this.dipendenti().reduce((somma, dipendente) => somma + (this.pagamentoDelMese(dipendente)?.importo ?? 0), 0),
  );

  /** Gli stipendi dei dipendenti attivi che nel mese scelto non risultano ancora pagati */
  readonly daPagare = computed(() =>
    this.dipendenti()
      .filter((dipendente) => dipendente.attivo && !this.pagamentoDelMese(dipendente))
      .reduce((somma, dipendente) => somma + dipendente.stipendioMensile, 0),
  );

  readonly messaggioPagamento = computed(
    () => `Stipendio di ${this.dataSelezionata().toLocaleDateString('it-IT', { month: 'long', year: 'numeric' })}.`,
  );

  private richiestaInCorso?: Subscription;

  constructor() {
    effect(() => {
      const spazioId = this.spaziLavoro.spazioAttivoId();
      if (spazioId === null) {
        return;
      }
      untracked(() => {
        this.annullaModifica();
        this.espanso.set(null);
        this.messaggio.set(null);
        if (this.visibile()) {
          this.carica(spazioId);
        } else {
          this.dipendenti.set([]);
          this.pagamenti.set({});
        }
      });
    });
  }

  /** Il pagamento del mese scelto, se esiste */
  pagamentoDelMese(dipendente: Dipendente): PagamentoStipendio | undefined {
    return (this.pagamenti()[dipendente.id] ?? []).find(
      (pagamento) => pagamento.mese === this.mese() && pagamento.anno === this.anno(),
    );
  }

  dataMese(mese: number, anno: number): Date {
    return new Date(anno, mese - 1, 1);
  }

  nonValido(campo: 'nome' | 'cognome' | 'stipendioMensile'): boolean {
    const controllo = this.modulo.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  alternaStorico(dipendente: Dipendente): void {
    this.espanso.update((attuale) => (attuale === dipendente.id ? null : dipendente.id));
  }

  modifica(dipendente: Dipendente): void {
    this.inModifica.set(dipendente.id);
    this.modulo.reset({
      nome: dipendente.nome,
      cognome: dipendente.cognome,
      mansione: dipendente.mansione ?? '',
      stipendioMensile: dipendente.stipendioMensile,
      dataAssunzione: dipendente.dataAssunzione ?? '',
      attivo: dipendente.attivo,
    });
  }

  annullaModifica(): void {
    this.inModifica.set(null);
    this.modulo.reset({
      nome: '',
      cognome: '',
      mansione: '',
      stipendioMensile: null,
      dataAssunzione: '',
      attivo: true,
    });
  }

  salva(): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const v = this.modulo.getRawValue();
    if (this.modulo.invalid || spazioId === null || v.stipendioMensile === null) {
      this.modulo.markAllAsTouched();
      return;
    }
    const richiesta: DipendenteRequest = {
      nome: v.nome.trim(),
      cognome: v.cognome.trim(),
      mansione: v.mansione.trim() || null,
      stipendioMensile: v.stipendioMensile,
      dataAssunzione: v.dataAssunzione || null,
      attivo: v.attivo,
    };

    this.salvataggioInCorso.set(true);
    this.errore.set(null);
    this.messaggio.set(null);
    const idModifica = this.inModifica();
    const chiamata =
      idModifica === null
        ? this.dipendenteService.crea(spazioId, richiesta)
        : this.dipendenteService.aggiorna(spazioId, idModifica, richiesta);

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

  /** Attiva o disattiva un dipendente, senza toccare gli altri dati */
  cambiaStato(dipendente: Dipendente): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.errore.set(null);
    this.messaggio.set(null);
    const richiesta: DipendenteRequest = {
      nome: dipendente.nome,
      cognome: dipendente.cognome,
      mansione: dipendente.mansione,
      stipendioMensile: dipendente.stipendioMensile,
      dataAssunzione: dipendente.dataAssunzione,
      attivo: !dipendente.attivo,
    };
    this.dipendenteService.aggiorna(spazioId, dipendente.id, richiesta).subscribe({
      next: () => this.carica(spazioId),
      error: (errore: unknown) => this.errore.set(messaggioErrore(errore)),
    });
  }

  apriPagamento(dipendente: Dipendente): void {
    this.pagamento.reset({
      importo: dipendente.stipendioMensile > 0 ? dipendente.stipendioMensile : null,
      dataPagamento: oggiIso(),
      metodoPagamento: 'BONIFICO',
    });
    this.errorePagamento.set(null);
    this.daPagare$.set(dipendente);
  }

  registraPagamento(dipendente: Dipendente): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    const v = this.pagamento.getRawValue();
    if (this.pagamento.invalid || spazioId === null || v.importo === null || this.pagamentoInCorso()) {
      return;
    }
    this.pagamentoInCorso.set(true);
    this.errorePagamento.set(null);

    this.dipendenteService
      .registraPagamento(spazioId, dipendente.id, {
        mese: this.mese(),
        anno: this.anno(),
        dataPagamento: v.dataPagamento,
        importo: v.importo,
        metodoPagamento: v.metodoPagamento,
      })
      .subscribe({
        next: () => {
          this.pagamentoInCorso.set(false);
          this.daPagare$.set(null);
          this.messaggio.set(`Stipendio di ${dipendente.nome} ${dipendente.cognome} registrato.`);
          this.carica(spazioId);
        },
        error: (errore: unknown) => {
          // Ad esempio 409: lo stipendio di questo mese è già stato registrato
          this.errorePagamento.set(messaggioErrore(errore));
          this.pagamentoInCorso.set(false);
        },
      });
  }

  eliminaConfermata(dipendente: Dipendente): void {
    const spazioId = this.spaziLavoro.spazioAttivoId();
    if (spazioId === null) {
      return;
    }
    this.eliminazioneInCorso.set(true);
    this.dipendenteService.elimina(spazioId, dipendente.id).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.carica(spazioId);
      },
      error: (errore: unknown) => {
        // 409 se ha pagamenti registrati: va disattivato
        this.eliminazioneInCorso.set(false);
        this.daEliminare.set(null);
        this.errore.set(messaggioErrore(errore));
      },
    });
  }

  /** Carica i dipendenti e, per ognuno, i pagamenti: una richiesta per dipendente, tutte insieme */
  private carica(spazioId: number): void {
    this.richiestaInCorso?.unsubscribe();
    this.caricamento.set(true);
    this.errore.set(null);

    this.richiestaInCorso = this.dipendenteService
      .elenca(spazioId)
      .pipe(
        switchMap((dipendenti) =>
          dipendenti.length === 0
            ? of({ dipendenti, pagamenti: [] as PagamentoStipendio[][] })
            : forkJoin(
              dipendenti.map((dipendente) => this.dipendenteService.elencaPagamenti(spazioId, dipendente.id)),
            ).pipe(map((pagamenti) => ({ dipendenti, pagamenti }))),
        ),
      )
      .subscribe({
        next: ({ dipendenti, pagamenti }) => {
          const perDipendente: Record<number, PagamentoStipendio[]> = {};
          dipendenti.forEach((dipendente, indice) => (perDipendente[dipendente.id] = pagamenti[indice]));
          this.dipendenti.set(dipendenti);
          this.pagamenti.set(perDipendente);
          this.caricamento.set(false);
        },
        error: (errore: unknown) => {
          this.errore.set(messaggioErrore(errore));
          this.caricamento.set(false);
        },
      });
  }
}
