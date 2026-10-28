// src/app/features/home/home.component.ts
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ElencoNotizieComponent } from '../../shared/componenti/elenco-notizie/elenco-notizie.component';

@Component({
  selector: 'app-home',
  imports: [RouterLink, ElencoNotizieComponent],
  template: `
    <header class="bg-white border-bottom sticky-top">
      <div class="container d-flex align-items-center justify-content-between py-2">
        <a routerLink="/" class="marchio text-decoration-none">
          <i class="bi bi-wallet2 me-2"></i>Personal Finance
        </a>
        <div class="d-flex gap-2">
          <a routerLink="/login" class="btn btn-outline-primary btn-sm">Accedi</a>
          <a routerLink="/registrazione" class="btn btn-primary btn-sm">Registrati</a>
        </div>
      </div>
    </header>

    <section class="presentazione">
      <div class="container py-5">
        <div class="row align-items-center">
          <div class="col-lg-8">
            <h1 class="display-5 fw-bold mb-3">Le tue finanze, sotto controllo.</h1>
            <p class="lead text-secondary mb-4">
              Gestisci entrate, uscite, budget e obiettivi di risparmio, da solo o con il tuo team:
              un unico spazio per le finanze personali e per quelle della tua azienda.
            </p>
            <div class="d-flex flex-wrap gap-2">
              <a routerLink="/registrazione" class="btn btn-primary btn-lg">Inizia gratis</a>
              <a routerLink="/login" class="btn btn-outline-secondary btn-lg">Ho già un account</a>
            </div>
          </div>
        </div>

        <div class="row g-3 mt-4">
          <div class="col-md-4">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <i class="bi bi-arrow-left-right fs-3 text-primary"></i>
                <h2 class="h6 mt-2">Transazioni, budget e obiettivi</h2>
                <p class="small text-secondary mb-0">
                  Registra ogni movimento, imposta i budget mensili e segui i tuoi obiettivi di risparmio.
                </p>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <i class="bi bi-people fs-3 text-primary"></i>
                <h2 class="h6 mt-2">Spazi personali e aziendali</h2>
                <p class="small text-secondary mb-0">
                  Invita colleghi con ruoli diversi e gestisci anche gli stipendi dei dipendenti.
                </p>
              </div>
            </div>
          </div>
          <div class="col-md-4">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <i class="bi bi-graph-up-arrow fs-3 text-primary"></i>
                <h2 class="h6 mt-2">Statistiche e grafici</h2>
                <p class="small text-secondary mb-0">
                  Una dashboard con l'andamento settimanale e mensile di entrate, uscite e saldo.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="container py-5" id="notizie">
      <h2 class="h3 mb-1">Economia internazionale</h2>
      <p class="text-secondary mb-4">Le ultime notizie dai principali media economici, aggiornate ogni pochi minuti.</p>
      <app-elenco-notizie [limite]="24" />
    </section>

    <footer class="border-top bg-white">
      <div class="container py-4 small text-secondary">
        Le notizie sono titoli e brevi estratti dei feed RSS pubblici delle testate indicate: ogni articolo
        rimanda al sito originale, a cui appartengono i contenuti.
      </div>
    </footer>
  `,
  styles: [
    `
      .marchio { font-weight: 700; font-size: 1.15rem; color: #0d6efd; }
      .presentazione { background: linear-gradient(135deg, #e7f0ff 0%, #f6f7f9 100%); }
    `,
  ],
})
export class HomeComponent {}
