/**
 * Development configuration (`ng serve`, `ng build --configuration development`).
 * `/api` is forwarded to http://localhost:8080 by the dev-server proxy (proxy.conf.json).
 */
export const environment = {
  production: false,
  apiUrl: '/api',
  oidc: {
    /** Keycloak realm started by infra/docker-compose.yml (must equal the `iss` claim). */
    authority: 'http://localhost:8180/realms/spendly',
    /** Public client declared in infra/keycloak/realm-export.json (Code + PKCE S256). */
    clientId: 'spendly-web',
  },
};
