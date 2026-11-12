import { Routes } from '@angular/router';

/** Lazy-loaded routes of the "accounting" feature (see app.routes.ts). */
export const ACCOUNTING_ROUTES: Routes = [
  {
    path: '',
    title: 'Comptabilité · Spendly',
    loadComponent: () => import('./accounting-page').then((m) => m.AccountingPage),
  },
];
