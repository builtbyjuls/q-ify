import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'status/probe'
  },
  {
    path: 'status',
    loadChildren: () =>
      import('./features/probe/probe.routes').then(({ PROBE_ROUTES }) => PROBE_ROUTES)
  }
];
