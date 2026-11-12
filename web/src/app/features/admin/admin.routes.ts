import { Routes } from '@angular/router';

/** Lazy-loaded routes of the "admin" feature (see app.routes.ts). */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Administration · Spendly',
    loadComponent: () => import('./admin-page').then((m) => m.AdminPage),
  },
];
