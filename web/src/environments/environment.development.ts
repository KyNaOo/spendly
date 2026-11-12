/**
 * Development configuration (`ng serve`, `ng build --configuration development`).
 * `/api` is forwarded to http://localhost:8080 by the dev-server proxy (proxy.conf.json).
 */
export const environment = {
  production: false,
  apiUrl: '/api',
};
