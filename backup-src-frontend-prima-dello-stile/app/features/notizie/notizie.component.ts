// src/app/features/notizie/notizie.component.ts
import { Component } from '@angular/core';

import { ElencoNotizieComponent } from '../../shared/componenti/elenco-notizie/elenco-notizie.component';

@Component({
  selector: 'app-notizie',
  imports: [ElencoNotizieComponent],
  template: `
    <h1 class="h3 mb-1">Notizie</h1>
    <p class="text-secondary mb-4">Economia internazionale, dalle principali testate.</p>
    <app-elenco-notizie [limite]="30" />
  `,
})
export class NotizieComponent {}
