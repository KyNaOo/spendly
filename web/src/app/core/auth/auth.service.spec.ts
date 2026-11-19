import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthenticatedResult, OidcSecurityService } from 'angular-auth-oidc-client';
import { BehaviorSubject, firstValueFrom, of, throwError } from 'rxjs';
import { AuthService, RETURN_URL_KEY } from './auth.service';

const LOGGED_OUT: AuthenticatedResult = { isAuthenticated: false, allConfigsAuthenticated: [] };
const LOGGED_IN: AuthenticatedResult = { isAuthenticated: true, allConfigsAuthenticated: [] };

/** Claims of an access token issued by the realm `spendly` for marc (EMPLOYEE + MANAGER). */
const MARC_CLAIMS = {
  sub: '00000000-0000-4000-8000-000000000003',
  preferred_username: 'marc',
  name: 'Marc Dubois',
  email: 'marc@spendly.local',
  realm_access: { roles: ['default-roles-spendly', 'offline_access', 'EMPLOYEE', 'MANAGER'] },
};

function setup() {
  const authenticated$ = new BehaviorSubject<AuthenticatedResult>(LOGGED_OUT);
  const oidc = {
    isAuthenticated$: authenticated$.asObservable(),
    getPayloadFromAccessToken: vi.fn(() => of(MARC_CLAIMS)),
    checkAuth: vi.fn(() => of({ isAuthenticated: true })),
    authorize: vi.fn(),
    logoff: vi.fn(() => of(null)),
    getAccessToken: vi.fn(() => of('')),
  };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: '**', children: [] }]),
      { provide: OidcSecurityService, useValue: oidc },
    ],
  });
  return { auth: TestBed.inject(AuthService), oidc, authenticated$ };
}

describe('AuthService', () => {
  afterEach(() => sessionStorage.clear());

  it('exposes an anonymous state while logged out', () => {
    const { auth } = setup();

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.user()).toBeNull();
    expect(auth.roles()).toEqual([]);
    expect(auth.hasAnyRole(['EMPLOYEE'])).toBe(false);
  });

  it('reads the user and the Spendly roles from the access token once authenticated', () => {
    const { auth, authenticated$ } = setup();

    authenticated$.next(LOGGED_IN);

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.user()).toEqual({
      id: MARC_CLAIMS.sub,
      username: 'marc',
      name: 'Marc Dubois',
      email: 'marc@spendly.local',
    });
    expect(auth.roles()).toEqual(['EMPLOYEE', 'MANAGER']);
    expect(auth.hasAnyRole(['MANAGER'])).toBe(true);
    expect(auth.hasAnyRole(['ACCOUNTANT', 'ADMIN'])).toBe(false);
  });

  it('resets the signals on logout', () => {
    const { auth, authenticated$ } = setup();
    authenticated$.next(LOGGED_IN);

    authenticated$.next(LOGGED_OUT);

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.roles()).toEqual([]);
  });

  it('stores the return URL and redirects to Keycloak on login', () => {
    const { auth, oidc } = setup();

    auth.login('/approvals');

    expect(sessionStorage.getItem(RETURN_URL_KEY)).toBe('/approvals');
    expect(oidc.authorize).toHaveBeenCalledOnce();
  });

  it('delegates logout to the OIDC library (RP-initiated logout)', () => {
    const { auth, oidc } = setup();

    auth.logout();

    expect(oidc.logoff).toHaveBeenCalledOnce();
  });

  it('returns null instead of an empty access token', async () => {
    const { auth, oidc } = setup();
    expect(await firstValueFrom(auth.accessToken())).toBeNull();

    oidc.getAccessToken.mockReturnValue(of('jwt'));
    expect(await firstValueFrom(auth.accessToken())).toBe('jwt');
  });

  it('goes back to the stored URL after the callback, once the first navigation is done', async () => {
    const { auth } = setup();
    const router = TestBed.inject(Router);
    sessionStorage.setItem(RETURN_URL_KEY, '/approvals');

    await auth.initialize();
    await router.navigateByUrl('/');

    await vi.waitFor(() => expect(router.url).toBe('/approvals'));
    expect(sessionStorage.getItem(RETURN_URL_KEY)).toBeNull();
  });

  it('starts anyway when Keycloak is unreachable', async () => {
    const { auth, oidc } = setup();
    oidc.checkAuth.mockReturnValue(throwError(() => new Error('network')));
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    await expect(auth.initialize()).resolves.toBeUndefined();

    expect(consoleError).toHaveBeenCalled();
    consoleError.mockRestore();
  });
});
