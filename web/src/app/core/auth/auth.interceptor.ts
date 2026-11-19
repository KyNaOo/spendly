import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { API_BASE_URL } from '../api/api-base-url';
import { AuthService } from './auth.service';

/**
 * Adds `Authorization: Bearer <access token>` to the requests sent to the Spendly API, and only
 * to them: the token must never leak to a third-party origin (CDN, Keycloak, MinIO presigned URL…).
 * Functional interceptor, registered with `provideHttpClient(withInterceptors([...]))`.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiUrl(req.url, inject(API_BASE_URL))) {
    return next(req);
  }
  return inject(AuthService)
    .accessToken()
    .pipe(
      switchMap((token) =>
        next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
      ),
    );
};

/** `true` for the base URL itself or any path below it (`/api`, `/api/me`, `/api?x`), not `/apix`. */
export function isApiUrl(url: string, apiBaseUrl: string): boolean {
  const base = apiBaseUrl.replace(/\/+$/, '');
  return url === base || url.startsWith(`${base}/`) || url.startsWith(`${base}?`);
}
