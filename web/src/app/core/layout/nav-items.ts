import { Role } from '../auth/role';

export interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly path: string;
  /**
   * Who sees the entry: `public` (everyone) or the roles allowed (at least one required).
   * Keep in line with the guards of app.routes.ts.
   */
  readonly access: 'public' | readonly Role[];
}

/** Main navigation entries, filtered by the layout according to the current user's roles. */
export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Accueil', icon: 'home', path: '/', access: 'public' },
  { label: 'Mes notes de frais', icon: 'receipt_long', path: '/reports', access: ['EMPLOYEE'] },
  { label: 'Validations', icon: 'fact_check', path: '/approvals', access: ['MANAGER'] },
  { label: 'Comptabilité', icon: 'account_balance', path: '/accounting', access: ['ACCOUNTANT'] },
  {
    label: 'Administration',
    icon: 'admin_panel_settings',
    path: '/admin',
    access: ['ADMIN'],
  },
];
