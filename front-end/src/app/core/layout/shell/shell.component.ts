import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { messaggioErrore } from '../../../shared/utilita/errori';
import { AutenticazioneService } from '../../services/autenticazione.service';
import { SpazioLavoroService } from '../../services/spazio-lavoro.service';
import { BadgeNotificheComponent } from '../../../shared/componenti/badge-notifiche/badge-notifiche.component';
import { PulsanteTemaComponent } from '../../../shared/componenti/pulsante-tema/pulsante-tema.component';

interface VoceMenu {
  percorso: string;
  etichetta: string;
  icona: string;
}

@Component({
  selector: 'app-shell',
  imports: [PulsanteTemaComponent, ReactiveFormsModule, RouterLink, RouterLinkActive, RouterOutlet, BadgeNotificheComponent],
  template: `
    <div class="contenitore">
      <!-- Barra superiore, solo su smartphone e tablet -->
      <header class="barra-mobile d-lg-none">
        <button type="button" class="btn btn-link text-dark p-0" aria-label="Apri il menu" (click)="menuAperto.set(true)">
          <i class="bi bi-list fs-2"></i>
        </button>
        <span class="fw-semibold">Personal Finance</span>
        <app-pulsante-tema class="ms-auto" />
      </header>

      @if (menuAperto()) {
        <div class="sfondo-menu d-lg-none" (click)="chiudiMenu()"></div>
      }

      <aside class="barra-laterale" [class.aperta]="menuAperto()">
        <div class="px-3 pt-3 pb-2">
          <div class="marchio"><i class="bi bi-wallet2 me-2"></i>Personal Finance</div>
        </div>

        <div class="px-3 pb-3">
          <label class="form-label small text-secondary mb-1" for="selettore-spazio">Spazio di lavoro</label>
          <select id="selettore-spazio" class="form-select form-select-sm" (change)="cambiaSpazio($event)">
            @for (spazio of spaziLavoro.elenco(); track spazio.id) {
              <option [value]="spazio.id" [selected]="spazio.id === spaziLavoro.spazioAttivoId()">
                {{ spazio.nome }} ({{ spazio.tipo === 'PERSONALE' ? 'personale' : 'azienda' }})
              </option>
            }
          </select>

          @if (mostraNuovoSpazio()) {
            <div class="mt-2">
              <input
                type="text"
                class="form-control form-control-sm mb-2"
                placeholder="Nome dell'azienda"
                maxlength="100"
                [formControl]="nomeNuovoSpazio"
                (keydown.enter)="creaSpazio()"
              />
              <div class="d-flex gap-2">
                <button
                  type="button"
                  class="btn btn-primary btn-sm flex-grow-1"
                  [disabled]="nomeNuovoSpazio.invalid || creazioneInCorso()"
                  (click)="creaSpazio()"
                >
                  Crea
                </button>
                <button type="button" class="btn btn-light btn-sm" (click)="mostraNuovoSpazio.set(false)">Annulla</button>
              </div>
              @if (erroreNuovoSpazio()) {
                <div class="text-danger small mt-1">{{ erroreNuovoSpazio() }}</div>
              }
            </div>
          } @else {
            <button type="button" class="btn btn-link btn-sm px-0 mt-1" (click)="mostraNuovoSpazio.set(true)">
              + Nuovo spazio aziendale
            </button>
          }
        </div>

        <nav class="px-2 flex-grow-1">
          @for (voce of voci(); track voce.percorso) {
            <a class="voce" [routerLink]="voce.percorso" routerLinkActive="attiva" (click)="chiudiMenu()">
              <i class="bi {{ voce.icona }}"></i>
              <span>{{ voce.etichetta }}</span>
              @if (voce.percorso === '/notifiche') {
                <app-badge-notifiche />
              }
            </a>
          }
        </nav>

        <div class="piede px-3 py-3">
          <div class="small fw-semibold text-truncate">{{ auth.utente()?.nome }} {{ auth.utente()?.cognome }}</div>
          <div class="small text-secondary text-truncate mb-2">{{ auth.utente()?.email }}</div>
          <app-pulsante-tema class="d-block mb-2" [conEtichetta]="true" />
          <button type="button" class="btn btn-outline-secondary btn-sm w-100" (click)="auth.esci()">
            <i class="bi bi-box-arrow-right me-1"></i>Esci
          </button>
        </div>
      </aside>

      <main class="contenuto">
        @if (erroreCaricamento()) {
          <div class="alert alert-danger">{{ erroreCaricamento() }}</div>
        }
        <router-outlet />
      </main>
    </div>
  `,
})
export class ShellComponent implements OnInit, OnDestroy {
  readonly auth = inject(AutenticazioneService);
  readonly spaziLavoro = inject(SpazioLavoroService);

  readonly menuAperto = signal(false);
  readonly mostraNuovoSpazio = signal(false);
  readonly creazioneInCorso = signal(false);
  readonly erroreNuovoSpazio = signal<string | null>(null);
  readonly erroreCaricamento = signal<string | null>(null);

  readonly nomeNuovoSpazio = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });

  /** Le voci cambiano con lo spazio attivo e con il ruolo dell'utente */
  readonly voci = computed<VoceMenu[]>(() => {
    const aziendale = this.spaziLavoro.spazioAttivo()?.tipo === 'AZIENDALE';
    const voci: VoceMenu[] = [
      { percorso: '/dashboard', etichetta: 'Dashboard', icona: 'bi-speedometer2' },
      { percorso: '/transazioni', etichetta: 'Transazioni', icona: 'bi-arrow-left-right' },
      { percorso: '/budget', etichetta: 'Budget', icona: 'bi-pie-chart' },
      { percorso: '/obiettivi', etichetta: 'Obiettivi', icona: 'bi-bullseye' },
      { percorso: '/categorie', etichetta: 'Categorie', icona: 'bi-tags' },
    ];
    if (aziendale && this.spaziLavoro.puoGestire()) {
      voci.push({ percorso: '/dipendenti', etichetta: 'Dipendenti', icona: 'bi-people' });
    }
    if (aziendale) {
      voci.push({ percorso: '/team', etichetta: 'Team', icona: 'bi-diagram-3' });
    }
    voci.push({ percorso: '/notizie', etichetta: 'Notizie', icona: 'bi-newspaper' });
    voci.push({ percorso: '/notifiche', etichetta: 'Notifiche', icona: 'bi-bell' });
    voci.push({ percorso: '/profilo', etichetta: 'Profilo', icona: 'bi-person-circle' });
    if (this.auth.isAdmin()) {
      voci.push({ percorso: '/admin', etichetta: 'Amministrazione', icona: 'bi-shield-lock' });
    }
    return voci;
  });

  ngOnInit(): void {
    this.spaziLavoro.carica().subscribe({
      error: (errore: unknown) => this.erroreCaricamento.set(messaggioErrore(errore)),
    });
  }

  ngOnDestroy(): void {
    this.spaziLavoro.reset();
  }

  chiudiMenu(): void {
    this.menuAperto.set(false);
  }

  cambiaSpazio(evento: Event): void {
    const id = Number((evento.target as HTMLSelectElement).value);
    this.spaziLavoro.seleziona(id);
  }

  creaSpazio(): void {
    const nome = this.nomeNuovoSpazio.value.trim();
    if (!nome || this.creazioneInCorso()) {
      return;
    }
    this.creazioneInCorso.set(true);
    this.erroreNuovoSpazio.set(null);

    this.spaziLavoro.creaAziendale(nome).subscribe({
      next: () => {
        this.nomeNuovoSpazio.reset();
        this.mostraNuovoSpazio.set(false);
        this.creazioneInCorso.set(false);
      },
      error: (errore: unknown) => {
        this.erroreNuovoSpazio.set(messaggioErrore(errore));
        this.creazioneInCorso.set(false);
      },
    });
  }
}
