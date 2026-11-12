export interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly path: string;
}

/** Main navigation entries. Filtering by role comes with authentication (Jalon 1). */
export const NAV_ITEMS: readonly NavItem[] = [
  { label: 'Accueil', icon: 'home', path: '/' },
  { label: 'Mes notes de frais', icon: 'receipt_long', path: '/reports' },
  { label: 'Validations', icon: 'fact_check', path: '/approvals' },
  { label: 'Comptabilité', icon: 'account_balance', path: '/accounting' },
  { label: 'Administration', icon: 'admin_panel_settings', path: '/admin' },
];
