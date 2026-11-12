import { Routes } from '@angular/router';

/** Lazy-loaded routes of the "reports" feature (see app.routes.ts). */
export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    title: 'Mes notes de frais · Spendly',
    loadComponent: () => import('./reports-page').then((m) => m.ReportsPage),
  },
];
