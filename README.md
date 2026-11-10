# Spendly

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

## Documentation

- [Cahier des charges](docs/02-notes-de-frais-spring-angular.md)
- [Décisions d'architecture (ADR)](docs/adr/)
- [Guide de contribution](CONTRIBUTING.md)
