import { Routes } from '@angular/router';

/** Lazy-loaded routes of the "approvals" feature (see app.routes.ts). */
export const APPROVALS_ROUTES: Routes = [
  {
    path: '',
    title: 'Validations · Spendly',
    loadComponent: () => import('./approvals-page').then((m) => m.ApprovalsPage),
  },
];
