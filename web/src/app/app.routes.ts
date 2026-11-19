import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guards';

/**
 * Root routes. Every feature is lazy-loaded (`loadChildren` / `loadComponent`):
 * its code lives in a separate bundle downloaded on first navigation.
 *
 * Home and the 403 page are public; every feature requires a login (`authGuard`, checked on each
 * child navigation) and a role (`roleGuard`), consistent with the menu (core/layout/nav-items.ts).
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Spendly',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'forbidden',
    title: 'Accès refusé · Spendly',
    loadComponent: () => import('./features/forbidden/forbidden-page').then((m) => m.ForbiddenPage),
  },
  {
    path: '',
    canActivateChild: [authGuard],
    children: [
      {
        path: 'reports',
        canActivate: [roleGuard(['EMPLOYEE'])],
        loadChildren: () =>
          import('./features/reports/reports.routes').then((m) => m.REPORTS_ROUTES),
      },
      {
        path: 'approvals',
        canActivate: [roleGuard(['MANAGER'])],
        loadChildren: () =>
          import('./features/approvals/approvals.routes').then((m) => m.APPROVALS_ROUTES),
      },
      {
        path: 'accounting',
        canActivate: [roleGuard(['ACCOUNTANT'])],
        loadChildren: () =>
          import('./features/accounting/accounting.routes').then((m) => m.ACCOUNTING_ROUTES),
      },
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMIN'])],
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
