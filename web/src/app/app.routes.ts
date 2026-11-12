import { Routes } from '@angular/router';

/**
 * Root routes. Every feature is lazy-loaded (`loadChildren` / `loadComponent`):
 * its code lives in a separate bundle downloaded on first navigation.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Spendly',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'reports',
    loadChildren: () => import('./features/reports/reports.routes').then((m) => m.REPORTS_ROUTES),
  },
  {
    path: 'approvals',
    loadChildren: () =>
      import('./features/approvals/approvals.routes').then((m) => m.APPROVALS_ROUTES),
  },
  {
    path: 'accounting',
    loadChildren: () =>
      import('./features/accounting/accounting.routes').then((m) => m.ACCOUNTING_ROUTES),
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
