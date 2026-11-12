/**
 * Production configuration (default `ng build`).
 * The SPA and the API are served from the same origin (reverse proxy), hence a relative URL.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
};
