import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { filter, firstValueFrom, map, Observable, of, switchMap, take } from 'rxjs';
import { isRole, Role } from './role';

/** Authenticated user, as described by the access token. */
export interface CurrentUser {
  /** Keycloak `sub`: also the primary key of the `employee` table (ADR 0003). */
  readonly id: string;
  readonly username: string;
  readonly name: string;
  readonly email?: string;
}

/** Subset of the Keycloak access token claims used by the front. */
interface AccessTokenClaims {
  readonly sub: string;
  readonly preferred_username?: string;
  readonly name?: string;
  readonly email?: string;
  readonly realm_access?: { readonly roles?: readonly string[] };
}

/** `sessionStorage` key of the URL to restore once back from the Keycloak login page. */
export const RETURN_URL_KEY = 'spendly.returnUrl';

/**
 * Facade over `angular-auth-oidc-client`: the rest of the application only sees signals
 * (`isAuthenticated`, `user`, `roles`) and three actions (`login`, `logout`, `accessToken`).
 * The roles shown here only drive the UI: the API checks them again on every request.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oidc = inject(OidcSecurityService);
  private readonly router = inject(Router);

  /** Claims of the current access token, `null` when logged out. */
  private readonly claims = toSignal(
    this.oidc.isAuthenticated$.pipe(
      switchMap(({ isAuthenticated }) =>
        isAuthenticated
          ? (this.oidc.getPayloadFromAccessToken() as Observable<AccessTokenClaims | null>)
          : of(null),
      ),
    ),
    { initialValue: null },
  );

  readonly isAuthenticated = computed(() => this.claims() !== null);

  readonly user = computed<CurrentUser | null>(() => {
    const claims = this.claims();
    if (!claims) {
      return null;
    }
    const username = claims.preferred_username ?? claims.sub;
    return { id: claims.sub, username, name: claims.name ?? username, email: claims.email };
  });

  readonly roles = computed<readonly Role[]>(() =>
    (this.claims()?.realm_access?.roles ?? []).filter(isRole),
  );

  /** `true` if the user holds at least one of the given roles. */
  hasAnyRole(roles: readonly Role[]): boolean {
    const granted = this.roles();
    return roles.some((role) => granted.includes(role));
  }

  /**
   * Called once by the app initializer: processes the Keycloak callback or restores the stored
   * session. Never rejects, so the application still starts when Keycloak is unreachable.
   */
  async initialize(): Promise<void> {
    try {
      const { isAuthenticated } = await firstValueFrom(this.oidc.checkAuth());
      if (isAuthenticated) {
        this.restoreReturnUrl();
      }
    } catch (error) {
      console.error('OIDC initialization failed', error);
    }
  }

  /** Redirects to the Keycloak login page; `returnUrl` is displayed again after the callback. */
  login(returnUrl: string = this.router.url): void {
    sessionStorage.setItem(RETURN_URL_KEY, returnUrl);
    this.oidc.authorize();
  }

  /** Ends the Keycloak session (RP-initiated logout) and comes back to the home page. */
  logout(): void {
    this.oidc.logoff().subscribe();
  }

  /** Current access token, `null` when logged out. */
  accessToken(): Observable<string | null> {
    return this.oidc.getAccessToken().pipe(
      take(1),
      map((token) => token || null),
    );
  }

  /**
   * The callback always lands on `/` (redirect URI); once the router has completed this first
   * navigation, go to the page that triggered the login.
   */
  private restoreReturnUrl(): void {
    const returnUrl = sessionStorage.getItem(RETURN_URL_KEY);
    sessionStorage.removeItem(RETURN_URL_KEY);
    if (!returnUrl || returnUrl === '/') {
      return;
    }
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        take(1),
      )
      .subscribe(() => void this.router.navigateByUrl(returnUrl));
  }
}
