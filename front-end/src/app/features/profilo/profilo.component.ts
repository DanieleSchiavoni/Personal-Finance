// src/app/features/profilo/profilo.component.ts
import { Component, inject, signal } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { switchMap } from 'rxjs';

import { AutenticazioneService } from '../../core/services/autenticazione.service';
import { UtenteService } from '../../core/services/utente.service';
import { ModaleConfermaComponent } from '../../shared/componenti/modale-conferma/modale-conferma.component';
import { messaggioErrore } from '../../shared/utilita/errori';
import { PATTERN_PASSWORD, campiUguali } from '../../shared/utilita/validatori';

@Component({
  selector: 'app-profilo',
  imports: [ReactiveFormsModule, ModaleConfermaComponent],
  template: `
    <h1 class="h3 mb-3">Profilo</h1>

    <div class="row g-4 larghezza-max-960">
      <div class="col-12 col-lg-6">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body p-4">
            <h2 class="h6 mb-3">Dati personali</h2>
            @if (messaggioDati()) {
              <div class="alert alert-success py-2">{{ messaggioDati() }}</div>
            }
            @if (erroreDati()) {
              <div class="alert alert-danger py-2">{{ erroreDati() }}</div>
            }
            <form [formGroup]="datiPersonali" (ngSubmit)="salvaDati()" novalidate>
              <div class="row g-3">
                <div class="col-sm-6">
                  <label class="form-label" for="p-nome">Nome</label>
                  <input id="p-nome" type="text" class="form-control" formControlName="nome"
                         [class.is-invalid]="nonValidoDati('nome')" />
                  <div class="invalid-feedback">Il nome è obbligatorio.</div>
                </div>
                <div class="col-sm-6">
                  <label class="form-label" for="p-cognome">Cognome</label>
                  <input id="p-cognome" type="text" class="form-control" formControlName="cognome"
                         [class.is-invalid]="nonValidoDati('cognome')" />
                  <div class="invalid-feedback">Il cognome è obbligatorio.</div>
                </div>
                <div class="col-12">
                  <label class="form-label" for="p-username">Username</label>
                  <input id="p-username" type="text" class="form-control" formControlName="username"
                         [class.is-invalid]="nonValidoDati('username')" />
                  <div class="invalid-feedback">Da 3 a 50 caratteri: lettere, numeri, punto, trattino e underscore.</div>
                </div>
                <div class="col-12">
                  <label class="form-label" for="p-email">Email</label>
                  <input id="p-email" type="email" class="form-control" formControlName="email"
                         [class.is-invalid]="nonValidoDati('email')" />
                  <div class="invalid-feedback">Inserisci un indirizzo email valido.</div>
                </div>
              </div>
              <button type="submit" class="btn btn-primary mt-3" [disabled]="salvataggioDati()">Salva</button>
            </form>
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-6">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-body p-4">
            <h2 class="h6 mb-3">Cambia password</h2>
            @if (messaggioPassword()) {
              <div class="alert alert-success py-2">{{ messaggioPassword() }}</div>
            }
            @if (errorePassword()) {
              <div class="alert alert-danger py-2">{{ errorePassword() }}</div>
            }
            <form [formGroup]="cambioPassword" (ngSubmit)="cambiaPasswordUtente()" novalidate>
              <div class="mb-3">
                <label class="form-label" for="pw-attuale">Password attuale</label>
                <input id="pw-attuale" type="password" class="form-control" formControlName="passwordAttuale"
                       autocomplete="current-password" [class.is-invalid]="nonValidaPassword('passwordAttuale')" />
                <div class="invalid-feedback">Inserisci la password attuale.</div>
              </div>
              <div class="mb-3">
                <label class="form-label" for="pw-nuova">Nuova password</label>
                <input id="pw-nuova" type="password" class="form-control" formControlName="nuovaPassword"
                       autocomplete="new-password" [class.is-invalid]="nonValidaPassword('nuovaPassword')" />
                <div class="invalid-feedback">Almeno 8 caratteri, con almeno una lettera e un numero.</div>
              </div>
              <div class="mb-3">
                <label class="form-label" for="pw-conferma">Conferma nuova password</label>
                <input id="pw-conferma" type="password" class="form-control" formControlName="conferma"
                       autocomplete="new-password" [class.is-invalid]="confermaNonValida()" />
                <div class="invalid-feedback">Le password non coincidono.</div>
              </div>
              <button type="submit" class="btn btn-primary" [disabled]="cambioInCorso()">Aggiorna password</button>
            </form>
          </div>
        </div>
      </div>

      <div class="col-12">
        <div class="card border border-danger-subtle shadow-sm">
          <div class="card-body p-4 d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div>
              <h2 class="h6 text-danger mb-1">Elimina account</h2>
              <p class="small text-secondary mb-0">
                Elimina il tuo account e gli spazi di cui sei l'unico membro, con tutti i loro dati. L'operazione non si può annullare.
              </p>
            </div>
            <button type="button" class="btn btn-outline-danger" (click)="apriEliminazione()">Elimina account</button>
          </div>
        </div>
      </div>
    </div>

    @if (mostraEliminazione()) {
      <app-modale-conferma
        titolo="Elimina account"
        messaggio="Per confermare inserisci la tua password."
        etichettaConferma="Elimina definitivamente"
        [inCorso]="eliminazioneInCorso()"
        [confermaDisabilitata]="passwordConferma.invalid"
        (confermato)="eliminaAccount()"
        (annullato)="mostraEliminazione.set(false)"
      >
        <input type="password" class="form-control mt-3" placeholder="Password" autocomplete="current-password"
               [formControl]="passwordConferma" />
        @if (erroreEliminazione()) {
          <div class="text-danger small mt-2">{{ erroreEliminazione() }}</div>
        }
      </app-modale-conferma>
    }
  `,
})
export class ProfiloComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly utenteService = inject(UtenteService);
  private readonly auth = inject(AutenticazioneService);

  readonly datiPersonali = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    cognome: ['', [Validators.required, Validators.maxLength(100)]],
    username: [
      '',
      [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9._-]+$/)],
    ],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
  });

  readonly cambioPassword = this.fb.group(
    {
      passwordAttuale: ['', [Validators.required]],
      nuovaPassword: [
        '',
        [Validators.required, Validators.minLength(8), Validators.maxLength(72), Validators.pattern(PATTERN_PASSWORD)],
      ],
      conferma: ['', [Validators.required]],
    },
    { validators: [campiUguali('nuovaPassword', 'conferma')] },
  );

  readonly passwordConferma = new FormControl('', { nonNullable: true, validators: [Validators.required] });

  readonly salvataggioDati = signal(false);
  readonly messaggioDati = signal<string | null>(null);
  readonly erroreDati = signal<string | null>(null);

  readonly cambioInCorso = signal(false);
  readonly messaggioPassword = signal<string | null>(null);
  readonly errorePassword = signal<string | null>(null);

  readonly mostraEliminazione = signal(false);
  readonly eliminazioneInCorso = signal(false);
  readonly erroreEliminazione = signal<string | null>(null);

  constructor() {
    const utente = this.auth.utente();
    if (utente) {
      this.datiPersonali.patchValue({
        nome: utente.nome,
        cognome: utente.cognome,
        username: utente.username,
        email: utente.email,
      });
    }
  }

  nonValidoDati(campo: 'nome' | 'cognome' | 'username' | 'email'): boolean {
    const controllo = this.datiPersonali.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  nonValidaPassword(campo: 'passwordAttuale' | 'nuovaPassword'): boolean {
    const controllo = this.cambioPassword.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }

  confermaNonValida(): boolean {
    const conferma = this.cambioPassword.controls.conferma;
    return (conferma.touched || conferma.dirty) && (conferma.invalid || this.cambioPassword.hasError('nonCoincidono'));
  }

  salvaDati(): void {
    if (this.datiPersonali.invalid) {
      this.datiPersonali.markAllAsTouched();
      return;
    }
    this.salvataggioDati.set(true);
    this.messaggioDati.set(null);
    this.erroreDati.set(null);

    const v = this.datiPersonali.getRawValue();
    this.utenteService
      .aggiorna({ username: v.username.trim(), email: v.email.trim(), nome: v.nome.trim(), cognome: v.cognome.trim() })
      .subscribe({
        next: (utente) => {
          this.auth.impostaUtente(utente);
          this.messaggioDati.set('Dati aggiornati.');
          this.salvataggioDati.set(false);
        },
        error: (errore: unknown) => {
          this.erroreDati.set(messaggioErrore(errore));
          this.salvataggioDati.set(false);
        },
      });
  }

  cambiaPasswordUtente(): void {
    if (this.cambioPassword.invalid) {
      this.cambioPassword.markAllAsTouched();
      return;
    }
    this.cambioInCorso.set(true);
    this.messaggioPassword.set(null);
    this.errorePassword.set(null);

    const v = this.cambioPassword.getRawValue();
    const username = this.auth.utente()?.username ?? '';

    this.utenteService
      .cambiaPassword({ passwordAttuale: v.passwordAttuale, nuovaPassword: v.nuovaPassword })
      .pipe(
        // Il backend chiude tutte le sessioni: si rientra subito con la nuova password
        switchMap(() => this.auth.login({ identificativo: username, password: v.nuovaPassword })),
      )
      .subscribe({
        next: () => {
          this.cambioPassword.reset();
          this.messaggioPassword.set('Password aggiornata. Le sessioni aperte su altri dispositivi sono state chiuse.');
          this.cambioInCorso.set(false);
        },
        error: (errore: unknown) => {
          this.errorePassword.set(messaggioErrore(errore));
          this.cambioInCorso.set(false);
        },
      });
  }

  apriEliminazione(): void {
    this.passwordConferma.reset();
    this.erroreEliminazione.set(null);
    this.mostraEliminazione.set(true);
  }

  eliminaAccount(): void {
    if (this.passwordConferma.invalid) {
      return;
    }
    this.eliminazioneInCorso.set(true);
    this.erroreEliminazione.set(null);

    this.utenteService.eliminaAccount(this.passwordConferma.value).subscribe({
      next: () => {
        this.eliminazioneInCorso.set(false);
        this.auth.esci();
      },
      error: (errore: unknown) => {
        // Ad esempio: password errata, oppure sei proprietario di uno spazio condiviso (409)
        this.erroreEliminazione.set(messaggioErrore(errore));
        this.eliminazioneInCorso.set(false);
      },
    });
  }
}
