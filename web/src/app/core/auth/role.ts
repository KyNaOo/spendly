/** Business roles carried by the Keycloak realm `spendly` (claim `realm_access.roles`). */
export const ROLES = ['EMPLOYEE', 'MANAGER', 'ACCOUNTANT', 'ADMIN'] as const;

export type Role = (typeof ROLES)[number];

/** Type guard: keeps only Spendly roles (Keycloak also adds `offline_access`, `default-roles-…`). */
export function isRole(value: unknown): value is Role {
  return (ROLES as readonly unknown[]).includes(value);
}
