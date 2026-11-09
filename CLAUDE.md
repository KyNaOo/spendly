# CLAUDE.md — Spendly

Application de **gestion des notes de frais avec circuit de validation** (employé → manager → comptabilité), projet portfolio orienté ESN / banque / assurance.

- **Cahier des charges complet** : `docs/02-notes-de-frais-spring-angular.md`. C'est la source de vérité : le relire avant toute décision d'architecture.
- **Conventions de contribution** (branches, commits, langues, PR) : `CONTRIBUTING.md`. **ADR** : `docs/adr/` (index dans `docs/adr/README.md`, ajouter une ligne à chaque nouvel ADR).
- **Dépôt GitHub** : `KyNaOo/spendly` (public). Tout le travail est découpé en issues.

## Démarrer une session

```bash
gh issue list --milestone "Jalon 0 — Socle"     # issues du jalon en cours
gh issue list --state open --limit 60            # vue d'ensemble
gh issue view <n>                                # tâches, critères d'acceptation, dépendances
```

- Travailler **jalon par jalon** (milestones « Jalon 0 » → « Jalon 6 »), dans l'ordre des numéros d'issue : les dépendances pointent toujours vers des issues de numéro inférieur (section « Dépendances » de chaque issue).
- Avant de commencer une issue, vérifier que ses dépendances sont fermées.
- Une issue = une branche `feat/<n>-slug` (ou `fix/`, `chore/`, `docs/`) = une PR qui la ferme (`Closes #<n>`).
- Cocher les cases de la checklist de l'issue au fil de l'avancement.
- Labels : `api`, `web`, `infra`, `ci-cd`, `domain`, `security`, `persistence`, `messaging`, `testing`, `observability`, `docs`.

## Stack

| Couche | Choix |
|---|---|
| Back | Java (dernière LTS), Spring Boot 3+, Maven (`./mvnw`), MapStruct, Flyway, PostgreSQL |
| Sécurité | Keycloak (realm `spendly`), Spring OAuth2 Resource Server (JWT), `@PreAuthorize` |
| Messaging | Kafka (KRaft) + pattern Transactional Outbox |
| Stockage | MinIO (S3), bucket `receipts` |
| Front | Angular (dernière stable), standalone, signals, Angular Material, NgRx SignalStore, `angular-auth-oidc-client` |
| Tests | JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit, RestAssured · Jest/Vitest + Testing Library · Playwright |
| Ops | Docker Compose, k3d + Helm, GitHub Actions, SonarCloud |

## Structure cible (monorepo)

```
api/     Spring Boot — com.spendly.{domain, application/port/{in,out}, application/service, adapter/{in/web, out/persistence, out/storage, out/messaging, out/pdf}, config}
web/     Angular — src/app/{core, shared, features/{reports, approvals, accounting, admin}}
infra/   docker-compose.yml, keycloak/realm-export.json, helm/spendly/
docs/    cahier des charges, ADR (docs/adr/)
.github/ workflows CI/CD
```

## Commandes (une fois le socle en place — mettre à jour si elles changent)

```bash
docker compose -f infra/docker-compose.yml up -d     # postgres, keycloak, mailpit, minio (+ kafka dès J4)
cd api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd api && ./mvnw verify                              # tests + Spotless/Checkstyle + ArchUnit
cd api && ./mvnw test -Dtest=ClassName               # un seul test
cd web && npm start                                  # http://localhost:4200 (proxy vers :8080)
cd web && npm run lint && npm test && npm run build
```

## Règles d'architecture (non négociables)

- **Hexagonal** : `domain` ne dépend **ni de Spring ni de JPA** (vérifié par ArchUnit). `application` ne dépend pas de `adapter`. Les entités JPA vivent dans `adapter/out/persistence` et sont mappées vers le domaine via MapStruct — jamais exposées ailleurs.
- **Argent** : value object `Money` (`BigDecimal` + `Currency`), **jamais de `double`**. Colonnes `numeric(19,4)`. Taux de change figé au jour de la dépense.
- **Machine à états** dans l'agrégat `ExpenseReport` :
  `DRAFT -submit-> SUBMITTED -approve-> APPROVED -pay-> REIMBURSED` ; `SUBMITTED -reject-> DRAFT` (motif obligatoire) ; `DRAFT|SUBMITTED -cancel-> CANCELLED`.
- **Règles métier** : soumission impossible sans ligne ni justificatif au-dessus du seuil ; ligne hors plafond **signalée, pas bloquée** ; pas d'auto-validation ; seul **le manager de l'auteur** valide.
- **Concurrence** : verrou optimiste `@Version` → HTTP 409.
- **Erreurs API** : `ProblemDetail` (RFC 7807) via `@RestControllerAdvice`, erreurs de validation détaillées par champ.
- **Événements** : écrits dans la table outbox dans la même transaction que l'agrégat, publiés ensuite ; consumers idempotents (clé `eventId`) + dead-letter topic.
- **Transactions** : `@Transactional` sur les services applicatifs (attention à l'auto-invocation).
- **Schéma BDD** : uniquement via migrations Flyway ; `ddl-auto=validate`.

## Conventions de code

- Code, noms de classes et commits en **anglais** ; issues, docs et README en **français**.
- Java moderne : `record` pour DTO/commandes/value objects, `sealed` + pattern matching pour événements et erreurs métier, `Optional` en retour (jamais en paramètre/champ).
- Angular : composants standalone, `input()`/`output()`, signals/`computed`, `OnPush`, nouveau control flow (`@if`, `@for`), guards et intercepteurs fonctionnels, formulaires réactifs typés.
- Tests : domaine testé **sans Spring** ; persistance avec Testcontainers PostgreSQL (**pas H2**) ; tout nouvel endpoint testé sur ses cas autorisé/interdit.
- Commits : Conventional Commits (`feat(api): …`, `fix(web): …`, `chore(infra): …`).
- Documenter les choix structurants dans `docs/adr/` (outbox, verrou optimiste, séparation domaine/JPA, gestion du `managerId`).

## Contexte développeur

Le développeur vient de PHP 8 / Symfony et apprend Java/Spring avec ce projet : expliquer brièvement les concepts Java/Spring non triviaux (et l'équivalent Symfony quand c'est utile) plutôt que de livrer du code opaque. Les points « entretien » du §11 du cahier des charges (N+1, outbox, PKCE, `@Transactional`, verrous, signals vs RxJS, `BigDecimal`) doivent rester démontrables dans le code et le README.
