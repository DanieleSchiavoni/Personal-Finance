// src/app/features/auth/registrazione/registrazione.component.ts
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AutenticazioneService } from '../../../core/services/autenticazione.service';
import { RegistrazioneRequest } from '../../../models/autenticazione.model';
import { messaggioErrore } from '../../../shared/utilita/errori';

/** Le due password devono coincidere */
const passwordUguali: ValidatorFn = (gruppo: AbstractControl): ValidationErrors | null => {
  const password = gruppo.get('password')?.value;
  const conferma = gruppo.get('confermaPassword')?.value;
  return password === conferma ? null : { nonCoincidono: true };
};

type Campo = 'username' | 'email' | 'password' | 'confermaPassword' | 'nome' | 'cognome';

@Component({
  selector: 'app-registrazione',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="d-flex align-items-center justify-content-center min-vh-100 p-3">
      <div class="card shadow-sm border-0 w-100" style="max-width: 480px">
        <div class="card-body p-4">
          <div class="text-center mb-4">
            <i class="bi bi-wallet2 text-primary fs-1"></i>
            <h1 class="h4 mt-2 mb-1">Crea il tuo account</h1>
            <p class="text-secondary small mb-0">Gestisci le tue finanze, personali o aziendali</p>
          </div>

          @if (errore()) {
            <div class="alert alert-danger py-2" role="alert">{{ errore() }}</div>
          }

          <form [formGroup]="modulo" (ngSubmit)="registrati()" novalidate>
            <div class="row g-3">
              <div class="col-sm-6">
                <label for="nome" class="form-label">Nome</label>
                <input id="nome" type="text" class="form-control" formControlName="nome"
                       autocomplete="given-name" [class.is-invalid]="nonValido('nome')" />
                <div class="invalid-feedback">Il nome è obbligatorio.</div>
              </div>
              <div class="col-sm-6">
                <label for="cognome" class="form-label">Cognome</label>
                <input id="cognome" type="text" class="form-control" formControlName="cognome"
                       autocomplete="family-name" [class.is-invalid]="nonValido('cognome')" />
                <div class="invalid-feedback">Il cognome è obbligatorio.</div>
              </div>
              <div class="col-12">
                <label for="username" class="form-label">Username</label>
                <input id="username" type="text" class="form-control" formControlName="username"
                       autocomplete="username" [class.is-invalid]="nonValido('username')" />
                <div class="invalid-feedback">
                  Da 3 a 50 caratteri: lettere, numeri, punto, trattino e underscore.
                </div>
              </div>
              <div class="col-12">
                <label for="email" class="form-label">Email</label>
                <input id="email" type="email" class="form-control" formControlName="email"
                       autocomplete="email" [class.is-invalid]="nonValido('email')" />
                <div class="invalid-feedback">Inserisci un indirizzo email valido.</div>
              </div>
              <div class="col-12">
                <label for="password" class="form-label">Password</label>
                <input id="password" type="password" class="form-control" formControlName="password"
                       autocomplete="new-password" [class.is-invalid]="nonValido('password')" />
                <div class="invalid-feedback">
                  Almeno 8 caratteri, con almeno una lettera e un numero.
                </div>
              </div>
              <div class="col-12">
                <label for="confermaPassword" class="form-label">Conferma password</label>
                <input id="confermaPassword" type="password" class="form-control" formControlName="confermaPassword"
                       autocomplete="new-password" [class.is-invalid]="nonCoincidono()" />
                <div class="invalid-feedback">Le password non coincidono.</div>
              </div>
            </div>

            <button type="submit" class="btn btn-primary w-100 mt-4" [disabled]="inCorso()">
              @if (inCorso()) {
                <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
              }
              Registrati
            </button>
          </form>

          <p class="text-center small text-secondary mt-4 mb-0">
            Hai già un account? <a routerLink="/login">Accedi</a>
          </p>
        </div>
      </div>
    </div>
  `,
})
export class RegistrazioneComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AutenticazioneService);
  private readonly router = inject(Router);

  readonly modulo = this.fb.group(
    {
      nome: ['', [Validators.required, Validators.maxLength(100)]],
      cognome: ['', [Validators.required, Validators.maxLength(100)]],
      username: [
        '',
        [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9._-]+$/)],
      ],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      password: [
        '',
        [Validators.required, Validators.minLength(8), Validators.maxLength(72), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)],
      ],
      confermaPassword: ['', [Validators.required]],
    },
    { validators: [passwordUguali] },
  );

  readonly inCorso = signal(false);
  readonly errore = signal<string | null>(null);

  registrati(): void {
    if (this.modulo.invalid) {
      this.modulo.markAllAsTouched();
      return;
    }
    this.inCorso.set(true);
    this.errore.set(null);

    const valori = this.modulo.getRawValue();
    const richiesta: RegistrazioneRequest = {
      username: valori.username,
      email: valori.email,
      password: valori.password,
      nome: valori.nome,
      cognome: valori.cognome,
    };

    this.auth.registra(richiesta).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
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

  /** La conferma è sbagliata se è stata toccata e le due password sono diverse */
  nonCoincidono(): boolean {
    const conferma = this.modulo.controls.confermaPassword;
    return (conferma.touched || conferma.dirty) && (conferma.invalid || this.modulo.hasError('nonCoincidono'));
  }
}
