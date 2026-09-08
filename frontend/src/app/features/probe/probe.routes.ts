import { Routes } from '@angular/router';

export const PROBE_ROUTES: Routes = [
  {
    path: 'probe',
    loadComponent: () => import('./probe.component').then(({ ProbeComponent }) => ProbeComponent)
  }
];
