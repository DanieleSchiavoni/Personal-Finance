import { Routes } from '@angular/router';

import { adminGuard } from './core/guards/admin.guard';
import { autenticatoGuard } from './core/guards/autenticato.guard';
import { homeGuard } from './core/guards/home.guard';
import { ospiteGuard } from './core/guards/ospite.guard';

const caricaSegnaposto = () =>
  import('./shared/componenti/segnaposto/segnaposto.component').then((m) => m.SegnapostoComponent);

const caricaElencoTransazioni = () =>
  import('./features/transazioni/elenco/transazioni-elenco.component').then((m) => m.TransazioniElencoComponent);

const caricaModuloTransazione = () =>
  import('./features/transazioni/modulo/transazione-modulo.component').then((m) => m.TransazioneModuloComponent);

export const routes: Routes = [
  {
    // Home pubblica con le notizie. Chi ha già una sessione va direttamente alla dashboard.
    path: '',
    pathMatch: 'full',
    canActivate: [homeGuard],
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'login',
    canActivate: [ospiteGuard],
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registrazione',
    canActivate: [ospiteGuard],
    loadComponent: () =>
      import('./features/auth/registrazione/registrazione.component').then((m) => m.RegistrazioneComponent),
  },
  {
    // L'area riservata: richiede l'accesso e usa il layout con la sidebar
    path: '',
    canActivate: [autenticatoGuard],
    loadComponent: () => import('./core/layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      { path: 'transazioni', loadComponent: caricaElencoTransazioni },
      { path: 'transazioni/nuova', loadComponent: caricaModuloTransazione },
      { path: 'transazioni/:id/modifica', loadComponent: caricaModuloTransazione },
      { path: 'budget', loadComponent: caricaSegnaposto, data: { titolo: 'Budget' } },
      { path: 'obiettivi', loadComponent: caricaSegnaposto, data: { titolo: 'Obiettivi di risparmio' } },
      {
        path: 'categorie',
        loadComponent: () => import('./features/categorie/categorie.component').then((m) => m.CategorieComponent),
      },
      { path: 'dipendenti', loadComponent: caricaSegnaposto, data: { titolo: 'Dipendenti' } },
      {
        path: 'team',
        loadComponent: () => import('./features/team/team.component').then((m) => m.TeamComponent),
      },
      {
        path: 'notizie',
        loadComponent: () => import('./features/notizie/notizie.component').then((m) => m.NotizieComponent),
      },
      {
        path: 'notifiche',
        loadComponent: () => import('./features/notifiche/notifiche.component').then((m) => m.NotificheComponent),
      },
      {
        path: 'profilo',
        loadComponent: () => import('./features/profilo/profilo.component').then((m) => m.ProfiloComponent),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/admin.component').then((m) => m.AdminComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
