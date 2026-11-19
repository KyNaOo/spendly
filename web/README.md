# Spendly — front Angular (`web/`)

Application Angular 22 (composants standalone, signals, zoneless, `OnPush`), Angular Material 3.
Voir le [README racine](../README.md) pour l'ensemble du projet.

## Commandes

```bash
npm ci                 # installation (Node ^22.22 || ^24.15 || >=26)
npm start              # http://localhost:4200 — /api et /actuator relayés vers http://localhost:8080 (proxy.conf.json)
npm test               # tests unitaires (Vitest + Angular Testing Library), une passe
npm run test:watch     # tests en mode watch
npm run lint           # ESLint (angular-eslint) + vérification Prettier
npm run format         # reformate avec Prettier
npm run build          # build de production dans dist/web/
```

## Arborescence

```
src/
├── environments/            # environment.ts (prod) / environment.development.ts (ng serve) : apiUrl, oidc
├── test-setup.ts            # matchers jest-dom pour Vitest
├── testing/                 # doublures de test (FakeAuthService), exclues du build
└── app/
    ├── core/                # singletons : layout (toolbar + sidenav filtrée par rôle), API_BASE_URL, auth/ (OIDC, intercepteur, guards)
    ├── shared/              # composants réutilisables (ui/page-header…)
    └── features/            # une route lazy-loadée par domaine fonctionnel
        ├── home/            # page d'accueil (publique)
        ├── forbidden/       # page 403 « Accès refusé » (cible de roleGuard)
        ├── reports/         # notes de frais de l'employé
        ├── approvals/       # file de validation du manager
        ├── accounting/      # remboursements et exports
        └── admin/           # administration
```

- Chaque feature expose un fichier `<feature>.routes.ts` chargé via `loadChildren` dans `app.routes.ts` : son code est un bundle séparé, téléchargé à la première navigation.
- L'URL de l'API s'injecte avec le jeton `API_BASE_URL` (`core/api/api-base-url.ts`) plutôt qu'en important `environment` partout, ce qui permet de la surcharger dans les tests.

## Authentification (`core/auth/`)

- `auth.config.ts` : `provideSpendlyAuth()` configure `angular-auth-oidc-client` (Code + PKCE, refresh token) et lance `checkAuth()` dans un _app initializer_, avant la première navigation.
- `auth.service.ts` : façade exposant `isAuthenticated`, `user`, `roles` en signals, et `login(returnUrl)`, `logout()`, `accessToken()`.
- `auth.interceptor.ts` : ajoute le Bearer aux seules requêtes vers `API_BASE_URL`.
- `auth.guards.ts` : `authGuard` et `roleGuard([...])` ; garder les rôles des routes (`app.routes.ts`) alignés sur ceux du menu (`core/layout/nav-items.ts`).
- Dans les tests, remplacer `AuthService` par `FakeAuthService` (`src/testing/`) : aucun Keycloak requis.
