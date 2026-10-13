// src/app/shared/componenti/badge-notifiche/badge-notifiche.component.ts
import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, switchMap, timer } from 'rxjs';

import { NotificaService } from '../../../core/services/notifica.service';

/** Il numero di notifiche non lette accanto alla voce di menu, aggiornato ogni minuto */
@Component({
  selector: 'app-badge-notifiche',
  template: `
    @if (notifiche.nonLette() > 0) {
      <span class="badge rounded-pill text-bg-danger ms-auto">{{ notifiche.nonLette() }}</span>
    }
  `,
})
export class BadgeNotificheComponent {
  readonly notifiche = inject(NotificaService);

  constructor() {
    timer(0, 60_000)
      .pipe(
        switchMap(() => this.notifiche.aggiornaConteggio().pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(),
      )
      .subscribe();
  }
}
