import { computed, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthService, CurrentUser } from '../app/core/auth/auth.service';
import { Role } from '../app/core/auth/role';

/**
 * Test double of AuthService (no Keycloak, no OIDC library): register it with
 * `{ provide: AuthService, useValue: fake }` and drive it with `signIn()` / `signOut()`.
 * Test-only file, excluded from the application build (tsconfig.app.json).
 */
export class FakeAuthService implements Pick<
  AuthService,
  'isAuthenticated' | 'user' | 'roles' | 'hasAnyRole'
> {
  private readonly current = signal<{ user: CurrentUser; roles: readonly Role[] } | null>(null);
  private token: string | null = null;

  readonly isAuthenticated = computed(() => this.current() !== null);
  readonly user = computed(() => this.current()?.user ?? null);
  readonly roles = computed(() => this.current()?.roles ?? []);

  /** Calls recorded for assertions. */
  readonly loginCalls: (string | undefined)[] = [];
  logoutCalls = 0;

  signIn(roles: readonly Role[], name = 'Alice Martin', token = 'fake-access-token'): this {
    this.current.set({ user: { id: 'sub-1', username: 'alice', name }, roles });
    this.token = token;
    return this;
  }

  signOut(): this {
    this.current.set(null);
    this.token = null;
    return this;
  }

  hasAnyRole(roles: readonly Role[]): boolean {
    const granted = this.roles();
    return roles.some((role) => granted.includes(role));
  }

  login(returnUrl?: string): void {
    this.loginCalls.push(returnUrl);
  }

  logout(): void {
    this.logoutCalls++;
    this.signOut();
  }

  accessToken(): Observable<string | null> {
    return of(this.token);
  }
}
