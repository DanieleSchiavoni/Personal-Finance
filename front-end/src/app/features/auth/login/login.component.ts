// src/app/features/auth/login/login.component.ts
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AutenticazioneService } from '../../../core/services/autenticazione.service';
import { messaggioErrore } from '../../../shared/utilita/errori';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="d-flex align-items-center justify-content-center min-vh-100 p-3">
      <div class="card shadow-sm border-0 w-100 larghezza-max-420">
        <div class="card-body p-4">
          <div class="text-center mb-4">
            <i class="bi bi-wallet2 text-primary fs-1"></i>
            <h1 class="h4 mt-2 mb-1">Personal Finance</h1>
            <p class="text-secondary small mb-0">Accedi al tuo account</p>
          </div>

          @if (errore()) {
            <div class="alert alert-danger py-2" role="alert">{{ errore() }}</div>
          }

          <form [formGroup]="modulo" (ngSubmit)="accedi()" novalidate>
            <div class="mb-3">
              <label for="identificativo" class="form-label">Username o email</label>
              <input
                id="identificativo"
                type="text"
                class="form-control"
                formControlName="identificativo"
                autocomplete="username"
                [class.is-invalid]="nonValido('identificativo')"
              />
              <div class="invalid-feedback">Inserisci il tuo username o la tua email.</div>
            </div>

            <div class="mb-4">
              <label for="password" class="form-label">Password</label>
              <input
                id="password"
                type="password"
                class="form-control"
                formControlName="password"
                autocomplete="current-password"
                [class.is-invalid]="nonValido('password')"
              />
              <div class="invalid-feedback">Inserisci la password.</div>
            </div>

            <button type="submit" class="btn btn-primary w-100" [disabled]="inCorso()">
              @if (inCorso()) {
                <span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>
              }
              Accedi
            </button>
          </form>

          <p class="text-center small text-secondary mt-4 mb-0">
            Non hai un account? <a routerLink="/registrazione">Registrati</a>
          </p>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AutenticazioneService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly modulo = this.fb.group({
    identificativo: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  readonly inCorso = signal(false);
  readonly errore = signal<string | null>(null);

  accedi(): void {
    if (this.modulo.invalid) {
      this.modulo.markAllAsTouched();
      return;
    }
    this.inCorso.set(true);
    this.errore.set(null);

    this.auth.login(this.modulo.getRawValue()).subscribe({
      next: () => {
        // Se si era stati rimandati al login da una pagina protetta, si torna lì
        const indirizzo = this.route.snapshot.queryParamMap.get('indirizzo');
        this.router.navigateByUrl(indirizzo && indirizzo.startsWith('/') ? indirizzo : '/dashboard');
      },
      error: (errore: unknown) => {
        this.errore.set(messaggioErrore(errore));
        this.inCorso.set(false);
      },
    });
  }

  nonValido(campo: 'identificativo' | 'password'): boolean {
    const controllo = this.modulo.controls[campo];
    return controllo.invalid && (controllo.touched || controllo.dirty);
  }
}
