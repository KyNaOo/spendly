import {
  EnvironmentProviders,
  inject,
  makeEnvironmentProviders,
  provideAppInitializer,
} from '@angular/core';
import { LogLevel, provideAuth } from 'angular-auth-oidc-client';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/**
 * OpenID Connect setup against Keycloak: Authorization Code flow with PKCE (S256), the only flow
 * allowed for the public client `spendly-web`. The access token (5 min) is renewed shortly before
 * expiry with the refresh token, without any iframe (`useRefreshToken`).
 *
 * The initializer runs `checkAuth()` before the first navigation: it handles the Keycloak
 * callback (`?code=…&state=…`, exchanged for tokens) or restores the session stored in
 * `sessionStorage`, so guards always see a settled authentication state.
 */
export function provideSpendlyAuth(): EnvironmentProviders {
  return makeEnvironmentProviders([
    provideAuth({
      config: {
        authority: environment.oidc.authority,
        clientId: environment.oidc.clientId,
        redirectUrl: window.location.origin,
        postLogoutRedirectUri: window.location.origin,
        scope: 'openid profile email',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        renewTimeBeforeTokenExpiresInSeconds: 30,
        // Keycloak issues a refresh token bound to the SSO session without `offline_access`.
        disableRefreshTokenOfflineAccessScopeWarning: true,
        // Keycloak does not repeat the nonce in the ID token returned by a refresh.
        ignoreNonceAfterRefresh: true,
        // User and roles are read from the access token claims: no extra /userinfo call.
        autoUserInfo: false,
        logLevel: environment.production ? LogLevel.Warn : LogLevel.Debug,
      },
    }),
    provideAppInitializer(() => inject(AuthService).initialize()),
  ]);
}
