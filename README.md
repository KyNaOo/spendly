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

_À venir (jalon 0) : `docker compose up` lancera l'ensemble des dépendances._

### API (`api/`)

Prérequis : JDK 25 et Docker (Maven est fourni par le wrapper `./mvnw`).

```bash
cd api
./mvnw verify                 # compilation, tests (PostgreSQL via Testcontainers), Spotless, Checkstyle
./mvnw spotless:apply         # reformate le code (google-java-format)
./mvnw spring-boot:test-run   # lance l'API avec un PostgreSQL éphémère (Testcontainers)
./mvnw spring-boot:run        # lance l'API contre la BDD de dev (localhost:5432/spendly, spendly/spendly)
```

Santé : `GET http://localhost:8080/actuator/health` (public) → `{"status":"UP"}` ; `/actuator/info` exige une authentification.
Identifiants BDD surchargeables via `SPENDLY_DB_URL`, `SPENDLY_DB_USERNAME`, `SPENDLY_DB_PASSWORD`.

## Documentation

- [Cahier des charges](docs/02-notes-de-frais-spring-angular.md)
- [Décisions d'architecture (ADR)](docs/adr/)
- [Guide de contribution](CONTRIBUTING.md)
