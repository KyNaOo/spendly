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
├── environments/            # environment.ts (prod) / environment.development.ts (ng serve) : apiUrl
├── test-setup.ts            # matchers jest-dom pour Vitest
└── app/
    ├── core/                # singletons : layout (toolbar + sidenav), API_BASE_URL… (auth, intercepteurs, guards à venir)
    ├── shared/              # composants réutilisables (ui/page-header…)
    └── features/            # une route lazy-loadée par domaine fonctionnel
        ├── home/            # page d'accueil
        ├── reports/         # notes de frais de l'employé
        ├── approvals/       # file de validation du manager
        ├── accounting/      # remboursements et exports
        └── admin/           # administration
```

- Chaque feature expose un fichier `<feature>.routes.ts` chargé via `loadChildren` dans `app.routes.ts` : son code est un bundle séparé, téléchargé à la première navigation.
- L'URL de l'API s'injecte avec le jeton `API_BASE_URL` (`core/api/api-base-url.ts`) plutôt qu'en important `environment` partout, ce qui permet de la surcharger dans les tests.
