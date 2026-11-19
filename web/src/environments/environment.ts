/**
 * Production configuration (default `ng build`).
 * The SPA and the API are served from the same origin (reverse proxy), hence a relative URL.
 * The OIDC authority is the public URL of the Keycloak realm (not a secret: the SPA is a
 * public client); it will be aligned with the deployed URL by the Kubernetes deployment.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
  oidc: {
    authority: 'http://localhost:8180/realms/spendly',
    clientId: 'spendly-web',
  },
};
