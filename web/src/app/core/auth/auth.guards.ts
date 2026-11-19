import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './role';

/**
 * Functional guard: lets authenticated users through, otherwise starts the Keycloak login
 * (the requested URL is restored after the callback) and cancels the navigation.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return true;
  }
  auth.login(state.url);
  return false;
};

/**
 * Guard factory: `roleGuard(['MANAGER'])` requires an authenticated user holding at least one of
 * the roles, and redirects to the 403 page otherwise. UI comfort only: the API is the real check.
 */
export function roleGuard(roles: readonly Role[]): CanActivateFn {
  return (route, state) => {
    const authenticated = authGuard(route, state);
    if (authenticated !== true) {
      return authenticated;
    }
    return inject(AuthService).hasAnyRole(roles) || inject(Router).createUrlTree(['/forbidden']);
  };
}
