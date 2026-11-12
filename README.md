# Spendly

[![api](https://github.com/KyNaOo/spendly/actions/workflows/api.yml/badge.svg?branch=main)](https://github.com/KyNaOo/spendly/actions/workflows/api.yml)
[![web](https://github.com/KyNaOo/spendly/actions/workflows/web.yml/badge.svg?branch=main)](https://github.com/KyNaOo/spendly/actions/workflows/web.yml)

> Gestion des notes de frais avec circuit de validation — Java / Spring Boot · Angular · Keycloak · Kafka · Kubernetes

🚧 **Projet en cours de construction** — voir l'avancement dans les [milestones](https://github.com/KyNaOo/spendly/milestones).

## Le pitch

Spendly remplace le fichier Excel des notes de frais d'une entreprise :

- un **employé** déclare ses dépenses (restaurant, transport, hôtel…) avec un justificatif ;
- son **manager** valide ou refuse, avec commentaire ;
- la **comptabilité** contrôle, rembourse et exporte les écritures du mois ;
- des **règles métier** (plafonds, TVA, devises) s'appliquent automatiquement ;
- tout est **tracé** (qui a fait quoi, quand).

## Stack

| Couche | Choix |
|---|---|
| Back | Java 25 (LTS), Spring Boot 4.1, Spring Data JPA, Spring Security, MapStruct, Flyway |
| Données | PostgreSQL, MinIO (S3) |
| Identité | Keycloak (OAuth2 / OIDC) |
| Messaging | Kafka (pattern Transactional Outbox) |
| Front | Angular, Angular Material, NgRx SignalStore |
| Tests | JUnit 5, Mockito, Testcontainers, ArchUnit, Playwright |
| Ops | Docker Compose, Kubernetes (Helm), GitHub Actions, SonarCloud |

## Structure du dépôt

```
api/      API Spring Boot (architecture hexagonale)
web/      Front Angular
infra/    Docker Compose, realm Keycloak, chart Helm
docs/     Cahier des charges et décisions d'architecture (ADR)
.github/  CI/CD, templates d'issues
```

## Démarrage rapide

### Dépendances locales (`infra/`)

Prérequis : Docker avec Compose v2.

```bash
docker compose -f infra/docker-compose.yml up -d      # démarre les services (premier lancement : ~1 min)
docker compose -f infra/docker-compose.yml ps -a      # état : services « healthy », minio-init « Exited (0) »
docker compose -f infra/docker-compose.yml down       # arrêt (ajouter -v pour effacer BDD et fichiers)
```

| Service | URL / port | Identifiants (dev) |
|---|---|---|
| PostgreSQL 18 | `localhost:5432`, base `spendly` | `spendly` / `spendly` |
| Keycloak — console d'administration | http://localhost:8180/admin | `admin` / `admin` |
| Keycloak — realm `spendly` (issuer) | http://localhost:8180/realms/spendly | — |
| Mailpit — interface web | http://localhost:8025 | — |
| Mailpit — SMTP | `localhost:1025` | — |
| MinIO — API S3 | http://localhost:9000, bucket `receipts` | `spendly` / `spendly-secret` |
| MinIO — navigateur d'objets | http://localhost:9001 | `spendly` / `spendly-secret` |

- Ports et identifiants surchargeables : copier `infra/.env.example` en `infra/.env` (ignoré par Git) et l'adapter. Les valeurs par défaut sont **réservées au développement**.
- Keycloak tourne en mode `start-dev` et importe au démarrage (`--import-realm`) les fichiers JSON de `infra/keycloak/`. Un realm déjà présent n'est pas réimporté : `docker compose … up -d --force-recreate keycloak` pour repartir du fichier.
- MinIO : le job ponctuel `minio-init` crée le bucket `receipts` (idempotent) dès que MinIO est sain, puis s'arrête avec le code 0. Pour cette raison, `up -d --wait` renvoie un code d'erreur alors que tout va bien : vérifier plutôt avec `ps -a`.
- L'édition communautaire de MinIO n'a plus de console d'administration complète : l'interface du port 9001 est un simple navigateur d'objets (buckets, fichiers). L'administration passe par `mc` (`docker compose -f infra/docker-compose.yml exec minio mc …`).
- Les images MinIO officielles n'étant plus téléchargeables, le compose utilise les images Chainguard épinglées par digest ([ADR 0002](docs/adr/0002-minio-image-source.md)).
- Les données vivent dans les volumes nommés `spendly_postgres-data` et `spendly_minio-data`.

### Keycloak : realm `spendly` et comptes de démo

Le realm est décrit dans [`infra/keycloak/realm-export.json`](infra/keycloak/realm-export.json) (fichier maintenu à la main, volontairement concis) et importé automatiquement au démarrage de Keycloak.

| Élément | Configuration |
|---|---|
| Rôles du realm | `EMPLOYEE`, `MANAGER`, `ACCOUNTANT`, `ADMIN` (claim `realm_access.roles` du jeton d'accès) |
| Client `spendly-web` | Client **public** (SPA Angular, pas de secret) : Authorization Code + **PKCE S256 obligatoire** ; redirect URIs `http://localhost:4200/*` ; origine CORS `http://localhost:4200` ; *password grant* désactivé |
| Client `spendly-api` | *Bearer-only* : ne sert qu'à nommer l'**audience** ; un mapper du client `spendly-web` ajoute `spendly-api` au claim `aud` des jetons d'accès |

Comptes de démo (mot de passe = identifiant, **développement uniquement**) :

| Identifiant | Nom | Rôles | `sub` (fixe) | Département / manager (en base, #15) |
|---|---|---|---|---|
| `alice` | Alice Martin | `EMPLOYEE` | `00000000-0000-4000-8000-000000000001` | Ventes, manager `marc` |
| `bob` | Bob Durand | `EMPLOYEE` | `00000000-0000-4000-8000-000000000002` | Ventes, manager `marc` |
| `marc` | Marc Lefèvre | `EMPLOYEE`, `MANAGER` | `00000000-0000-4000-8000-000000000003` | Ventes |
| `claire` | Claire Dubois | `EMPLOYEE`, `ACCOUNTANT` | `00000000-0000-4000-8000-000000000004` | Finance |
| `sophie` | Sophie Bernard | `ADMIN` | `00000000-0000-4000-8000-000000000005` | Direction des systèmes d'information |

- Le manager et le comptable ont aussi le rôle `EMPLOYEE` : ils déclarent leurs propres notes de frais.
- Keycloak ne porte que l'identité et les rôles. Le **département et la relation manager vivent en base** (table `employee`, clé = `sub` Keycloak) ; les `sub` fixes permettent aux données de démo de concorder avec le realm ([ADR 0003](docs/adr/0003-identity-and-manager-relationship.md)).
- Vérification : `infra/scripts/check-keycloak-realm.sh` (requiert `curl`, `jq`, `openssl`) joue le vrai flux Authorization Code + PKCE pour chaque compte et contrôle le `sub`, les rôles et l'audience du jeton, ainsi que le refus d'une requête sans PKCE et du *password grant*. Variable `KEYCLOAK_URL` si le port n'est pas 8180.
- Après modification de `realm-export.json` : `docker compose -f infra/docker-compose.yml up -d --force-recreate keycloak` (Keycloak n'a pas de volume : recréer le conteneur repart d'une base vierge et réimporte le fichier).

### API (`api/`)

Prérequis : JDK 25 et Docker (Maven est fourni par le wrapper `./mvnw`).

```bash
cd api
./mvnw verify                 # compilation, tests (PostgreSQL via Testcontainers), Spotless, Checkstyle
./mvnw spotless:apply         # reformate le code (google-java-format)
./mvnw spring-boot:test-run   # lance l'API avec un PostgreSQL éphémère (Testcontainers)
./mvnw spring-boot:run        # lance l'API en profil dev contre le Docker Compose (localhost:5432/spendly)
```

Santé : `GET http://localhost:8080/actuator/health` (public) → `{"status":"UP"}` ; `/actuator/info` exige une authentification.

#### Profils et configuration

| Profil | Usage | Activation |
|---|---|---|
| `dev` | Développement local contre `infra/docker-compose.yml` (valeurs par défaut de `infra/.env.example`) | Automatique avec `./mvnw spring-boot:run` / `spring-boot:test-run` |
| `test` | Tests automatisés ; la BDD est fournie par Testcontainers | `@ActiveProfiles("test")` |
| `prod` | Environnements déployés : **aucune valeur sensible en dur**, tout vient de variables d'environnement | `SPRING_PROFILES_ACTIVE=prod` |

- Le profil `dev` n'est actif par défaut **qu'en local**, via la propriété Maven `spring-boot.run.profiles` du `pom.xml` (surcharge : `./mvnw spring-boot:run -Dspring-boot.run.profiles=…`). Le jar packagé n'a aucun profil par défaut : lancé sans profil, il refuse de démarrer.
- Les réglages propres à Spendly sont typés dans `SpendlyProperties` (clés `spendly.*`), validés au démarrage : une propriété obligatoire manquante arrête l'application avec un message nommant la propriété.
- En `prod`, toute variable d'environnement manquante est signalée d'un coup au démarrage (« Missing configuration… »), avant la création du moindre bean.

| Variable | `dev` (défaut) | `prod` |
|---|---|---|
| `SPENDLY_DB_URL` | `jdbc:postgresql://localhost:5432/spendly` | obligatoire |
| `SPENDLY_DB_USERNAME` | `spendly` | obligatoire |
| `SPENDLY_DB_PASSWORD` | `spendly` | obligatoire |
| `SPENDLY_FRONTEND_URL` | `http://localhost:4200` | obligatoire |
| `SPENDLY_BASE_CURRENCY` | `EUR` | optionnelle (`EUR`) |

### Front (`web/`)

Prérequis : Node.js `^22.22`, `^24.15` ou `>=26` (contrainte d'Angular 22).

```bash
cd web
npm ci                # installation des dépendances
npm start             # http://localhost:4200 ; /api et /actuator sont relayés vers l'API (localhost:8080)
npm test              # tests unitaires (Vitest + Angular Testing Library) ; npm run test:watch en continu
npm run lint          # ESLint (angular-eslint) + vérification du formatage Prettier ; npm run format pour corriger
npm run build         # build de production (dist/web/)
```

- Angular 22 (standalone, signals, zoneless), Angular Material 3 (thème `azure`/`blue` dans `src/styles.scss`).
- Arborescence `core/` (layout, configuration), `shared/` (composants réutilisables), `features/{reports,approvals,accounting,admin}` chargées à la demande (*lazy loading*). Détails dans [`web/README.md`](web/README.md).
- L'URL de l'API vient de `src/environments/` (`apiUrl: '/api'`) : en développement, le proxy `web/proxy.conf.json` évite tout problème de CORS.

## Intégration continue

GitHub Actions (`.github/workflows/`), déclenché à chaque push sur `main` (pas de PR : projet solo) ou manuellement (`workflow_dispatch`) :

| Workflow | Déclenché si changement dans | Étapes |
|---|---|---|
| [`api`](.github/workflows/api.yml) | `api/**`, le workflow lui-même | JDK 25 Temurin, cache Maven, `./mvnw verify` (tests Testcontainers + ArchUnit, Spotless, Checkstyle) ; rapports de tests publiés en artefact en cas d'échec |
| [`web`](.github/workflows/web.yml) | `web/**`, le workflow lui-même | Node.js 26, cache npm, `npm ci`, `npm run lint`, `npm test` (Vitest/jsdom, sans navigateur), `npm run build` |

- Les filtres `paths` évitent de relancer le build Java pour un changement du front (et inversement) ; un push ne touchant que `docs/` ne lance rien.
- Un nouveau push annule le run encore en cours du même workflow (`concurrency`).
- Suivi : `gh run list` / `gh run watch`.

## Documentation

- [Cahier des charges](docs/02-notes-de-frais-spring-angular.md)
- [Décisions d'architecture (ADR)](docs/adr/)
- [Guide de contribution](CONTRIBUTING.md)
