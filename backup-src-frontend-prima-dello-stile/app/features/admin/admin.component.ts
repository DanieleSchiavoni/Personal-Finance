// src/app/features/admin/admin.component.ts
import { Component, signal } from '@angular/core';

import { AdminCategorieComponent } from './admin-categorie.component';
import { AdminStatisticheComponent } from './admin-statistiche.component';
import { AdminUtentiComponent } from './admin-utenti.component';

type Scheda = 'statistiche' | 'utenti' | 'categorie';

/** Contenitore a schede: ogni scheda è un componente separato */
@Component({
  selector: 'app-admin',
  imports: [AdminStatisticheComponent, AdminUtentiComponent, AdminCategorieComponent],
  template: `
    <h1 class="h3 mb-3">Amministrazione</h1>

    <ul class="nav nav-tabs mb-4">
      @for (voce of schede; track voce.id) {
        <li class="nav-item">
          <button type="button" class="nav-link" [class.active]="scheda() === voce.id" (click)="scheda.set(voce.id)">
            {{ voce.etichetta }}
          </button>
        </li>
      }
    </ul>

    @switch (scheda()) {
      @case ('statistiche') {
        <app-admin-statistiche />
      }
      @case ('utenti') {
        <app-admin-utenti />
      }
      @case ('categorie') {
        <app-admin-categorie />
      }
    }
  `,
})
export class AdminComponent {
  readonly schede: { id: Scheda; etichetta: string }[] = [
    { id: 'statistiche', etichetta: 'Statistiche' },
    { id: 'utenti', etichetta: 'Utenti' },
    { id: 'categorie', etichetta: 'Categorie globali' },
  ];
  readonly scheda = signal<Scheda>('statistiche');
}
