# Spendly — Gestion des notes de frais avec circuit de validation

> **Stack :** Java (dernière LTS) · Spring Boot 3+ · Spring Security + Keycloak · JPA/Hibernate · PostgreSQL · Kafka · Angular (dernière version stable) · Docker · Kubernetes · GitHub Actions
> **Cible marché :** ESN, banque, assurance, grands comptes — la combinaison la plus demandée du scan (Java/Spring + Angular = 34 % des offres fullstack, Java cité dans 38 % des offres)
> **Durée indicative :** 10 à 12 semaines à raison de ~15-20 h/semaine (estimation — Java est une nouvelle stack pour vous, prévoir une montée en compétence)

---

## 1. Le pitch

Spendly remplace le fichier Excel des notes de frais d'une entreprise :

- un **employé** déclare ses dépenses (restaurant, transport, hôtel…) avec un justificatif,
- son **manager** valide ou refuse, avec commentaire,
- la **comptabilité** contrôle, rembourse et exporte les écritures du mois,
- des **règles métier** (plafonds, TVA, devises) s'appliquent automatiquement,
- tout est **tracé** (qui a fait quoi, quand) : exigence classique en banque/assurance.

Nom de dépôt suggéré : `spendly` (ou deux dépôts `spendly-api` / `spendly-web` si vous préférez, mais un monorepo reste plus simple à présenter).

---

## 2. Ce que ce projet doit prouver à un recruteur

| Compétence | Où elle apparaît |
|---|---|
| Java moderne | Records, sealed interfaces, pattern matching, streams, `Optional`, `var` |
| Spring Boot « entreprise » | Couches propres, configuration par profil, validation, gestion d'erreurs |
| Sécurité | OAuth2/OIDC avec Keycloak, JWT resource server, sécurité par méthode |
| Persistance | JPA/Hibernate maîtrisé (relations, lazy loading, N+1, transactions, verrous) |
| Règles métier | Machine à états, domaine testé unitairement, architecture hexagonale |
| Messaging | Kafka (événements de domaine) |
| Angular | Composants standalone, signals, RxJS, formulaires réactifs complexes, Material |
| Qualité | JUnit 5, Mockito, Testcontainers, ArchUnit, tests Angular |
| Ops | Docker, Kubernetes (Helm), CI/CD, observabilité (Actuator, Prometheus) |

---

## 3. Stack technique

| Couche | Choix |
|---|---|
| Langage | Java (dernière LTS) |
| Build | Maven (le plus répandu en ESN) — Gradle en alternative |
| Framework | Spring Boot 3+ : Web, Data JPA, Validation, Security, OAuth2 Resource Server, Actuator, Kafka |
| Mapping | MapStruct |
| BDD | PostgreSQL + Flyway |
| Identité | Keycloak (conteneur) |
| Messaging | Kafka (mode KRaft, un seul broker en local) |
| PDF | OpenPDF ou JasperReports |
| Stockage fichiers | MinIO (API S3) |
| Doc API | springdoc-openapi |
| Front | Angular + Angular Material + NgRx SignalStore |
| Auth front | `angular-auth-oidc-client` (ou `keycloak-angular`) |
| Tests back | JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit, RestAssured |
| Tests front | Jest (ou Vitest) + Angular Testing Library, Playwright pour l'E2E |
| Qualité | Checkstyle/Spotless, SonarCloud (gratuit pour les projets publics) |
| Ops | Docker Compose, Kubernetes (k3d en local) + Helm, GitHub Actions |

---

## 4. Architecture

### Vue d'ensemble

```
Navigateur (Angular) ──OIDC──▶ Keycloak
      │ JWT
      ▼
┌──────────────────────────────────────────┐
│ spendly-api (Spring Boot)                │
│                                          │
│  adapters/in  : REST controllers         │
│  application  : use cases (services)     │
│  domain       : entités, règles, états   │
│  adapters/out : JPA, S3, Kafka, PDF      │
└──────┬─────────────┬───────────┬─────────┘
       │             │           │
  PostgreSQL       Kafka       MinIO
                     │
             ┌───────┴────────┐
             │ consumer :     │
             │ notifications, │
             │ audit          │
             └────────────────┘
```

### Architecture hexagonale (ports & adapters)

Le **domaine** ne dépend de rien (ni Spring, ni JPA). Les **use cases** orchestrent. Les **adapters** branchent le monde extérieur. Une règle ArchUnit vérifie cette dépendance en CI.

```
spendly/
├── api/
│   └── src/main/java/com/spendly/
│       ├── domain/
│       │   ├── expense/        # ExpenseReport, ExpenseLine, ReportStatus, règles
│       │   ├── policy/         # plafonds par catégorie
│       │   └── shared/         # Money, Currency, UserId (value objects)
│       ├── application/
│       │   ├── port/in/        # SubmitReportUseCase, ApproveReportUseCase…
│       │   ├── port/out/       # ReportRepository, ReceiptStorage, EventPublisher…
│       │   └── service/
│       ├── adapter/
│       │   ├── in/web/         # controllers, DTO, mappers, gestion d'erreurs
│       │   ├── out/persistence/ # entités JPA, repositories Spring Data
│       │   ├── out/storage/
│       │   ├── out/messaging/
│       │   └── out/pdf/
│       └── config/             # sécurité, Kafka, OpenAPI
│   └── src/main/resources/
│       ├── application.yml, application-dev.yml, application-prod.yml
│       └── db/migration/       # Flyway V1__init.sql…
├── web/                        # Angular
│   └── src/app/
│       ├── core/               # auth, intercepteurs, guards, layout
│       ├── shared/             # composants, pipes, directives
│       └── features/
│           ├── reports/        # liste, détail, formulaire
│           ├── approvals/      # file de validation manager
│           ├── accounting/     # remboursements, exports
│           └── admin/          # politiques de dépenses
├── infra/
│   ├── docker-compose.yml      # postgres, keycloak, kafka, minio, mailpit
│   ├── keycloak/realm-export.json
│   └── helm/spendly/
└── .github/workflows/
```

---

## 5. Modèle de domaine

```
ExpenseReport (id, employeeId, title, period (YearMonth), status, totalAmount: Money,
               submittedAt?, decidedAt?, decidedBy?, rejectionReason?, version)
ExpenseLine   (id, reportId, date, category, description, amount: Money,
               vatAmount: Money, currency, exchangeRate, receiptKey?)
Category      : MEAL | TRAVEL | LODGING | MILEAGE | OTHER
ExpensePolicy (id, category, maxAmountPerLine, requiresReceiptAbove, active)
Employee      (id = subject Keycloak, name, email, managerId, department)
AuditEntry    (id, aggregateId, actor, action, before jsonb, after jsonb, at)
Reimbursement (id, reportId, amount, paidAt, batchId)
```

### Machine à états

```
DRAFT ──submit──▶ SUBMITTED ──approve──▶ APPROVED ──pay──▶ REIMBURSED
  ▲                   │
  └────── reject ─────┘ (retour en DRAFT avec motif)
DRAFT / SUBMITTED ──cancel──▶ CANCELLED
```

**Règles métier (à implémenter dans le domaine, testées unitairement) :**

- Une note ne peut être soumise que si elle a au moins une ligne et que chaque ligne au-dessus du seuil a un justificatif.
- Un employé ne peut pas valider sa propre note ; seul **son** manager peut valider.
- Une ligne au-dessus du plafond de sa catégorie est **signalée** (pas bloquée) et le manager doit la justifier.
- Les montants sont en `BigDecimal` via un value object `Money` — **jamais de `double`** pour de l'argent.
- Conversion de devise figée au taux du jour de la dépense.
- Verrouillage optimiste (`@Version`) : deux managers ne peuvent pas décider en même temps.

---

## 6. API REST (extrait)

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/reports?status=&period=&page=&size=&sort=` | EMPLOYEE (les siennes) |
| POST | `/api/reports` | EMPLOYEE |
| PUT | `/api/reports/{id}` | propriétaire, si DRAFT |
| POST | `/api/reports/{id}/lines` | propriétaire |
| POST | `/api/reports/{id}/lines/{lineId}/receipt` (multipart) | propriétaire |
| POST | `/api/reports/{id}/submit` | propriétaire |
| GET | `/api/approvals` | MANAGER (son équipe) |
| POST | `/api/reports/{id}/approve` · `/reject` | MANAGER |
| POST | `/api/accounting/reimbursements` | ACCOUNTANT |
| GET | `/api/accounting/exports/{yearMonth}.csv` · `.pdf` | ACCOUNTANT |
| CRUD | `/api/admin/policies` | ADMIN |
| GET | `/api/reports/{id}/history` | participants |

Erreurs au format **Problem Details (RFC 7807)** via `@RestControllerAdvice` + `ProblemDetail`.

---

## 7. Feuille de route par jalons

### Jalon 0 — Remise à niveau Java et socle (semaines 1-2)

Avant de coder : faire le point sur Java moderne (records, streams, `Optional`, collections, exceptions, génériques, sealed/pattern matching). Venant de PHP 8 / Symfony, les notions DI, ORM et contrôleurs sont transposables.

- [ ] Projet Spring Boot généré avec start.spring.io (Web, JPA, Validation, Security, Actuator, Flyway, PostgreSQL)
- [ ] `docker-compose.yml` : postgres, keycloak (realm importé automatiquement), mailpit, minio
- [ ] Profils `dev` / `test` / `prod`, `@ConfigurationProperties` typées
- [ ] Projet Angular (standalone, routing, Material, ESLint, Prettier)
- [ ] CI : `mvn verify` + `ng lint` / `ng test` / `ng build`
- [ ] Actuator `/actuator/health`

**Concepts :** conteneur IoC, beans, scopes, autoconfiguration, profils, cycle de vie Maven.

### Jalon 1 — Sécurité (semaine 3)

- [ ] Realm Keycloak `spendly` : rôles `EMPLOYEE`, `MANAGER`, `ACCOUNTANT`, `ADMIN` ; 5 utilisateurs de démo
- [ ] Spring en **resource server** JWT ; conversion des rôles Keycloak en `GrantedAuthority`
- [ ] `SecurityFilterChain` : routes publiques, CORS, stateless
- [ ] `@PreAuthorize` sur les use cases, y compris règles fines (« seulement le manager de l'auteur »)
- [ ] Angular : login OIDC (flow Authorization Code + PKCE), intercepteur qui ajoute le token, guards par rôle, menu adapté au rôle

**Concepts :** OAuth2 vs OIDC, flows, PKCE, JWT (signature, claims, expiration), filtres Spring Security.

### Jalon 2 — Domaine et persistance (semaines 4-5)

- [ ] Domaine pur : `ExpenseReport` agrégat, `Money` value object, machine à états, exceptions métier
- [ ] Tests unitaires du domaine **sans Spring** (rapides)
- [ ] Entités JPA séparées du domaine + mappers MapStruct
- [ ] Relations `@OneToMany` / `@ManyToOne`, `cascade`, `orphanRemoval`, `fetch = LAZY`
- [ ] Mise en évidence puis correction d'un **problème N+1** (`@EntityGraph` ou `JOIN FETCH`) — à documenter dans le README
- [ ] `@Transactional` sur les use cases ; comprendre la propagation et le rollback
- [ ] `@Version` (verrou optimiste) + gestion de `OptimisticLockException` → HTTP 409
- [ ] Pagination et tri (`Pageable`), filtres dynamiques avec `Specification`
- [ ] Migrations Flyway versionnées, données de démo en migration `R__` ou profil `dev`

**Concepts :** unité de travail, contexte de persistance, dirty checking, lazy loading hors transaction, index.

### Jalon 3 — API REST et front de saisie (semaines 5-6)

- [ ] Controllers, DTO en `record`, Bean Validation (`@NotNull`, `@Positive`, validateur personnalisé)
- [ ] `@RestControllerAdvice` → `ProblemDetail`
- [ ] springdoc : Swagger UI sur `/swagger-ui`
- [ ] Upload des justificatifs vers MinIO (type/taille validés), téléchargement par URL présignée
- [ ] Angular : liste paginée (`mat-table` + `mat-paginator` + `mat-sort`) branchée sur l'API
- [ ] **Formulaire réactif complexe** : `FormArray` de lignes, total recalculé en direct, validateur personnalisé (plafond), validateur asynchrone, gestion des erreurs serveur champ par champ
- [ ] Upload avec barre de progression
- [ ] Services HTTP typés, gestion d'erreurs centralisée (intercepteur + snackbar)

**Concepts Angular :** composants standalone, `input()`/`output()`, signals et `computed`, change detection (`OnPush`), injection, pipes.

### Jalon 4 — Circuit de validation et événements (semaines 7-8)

- [ ] Endpoints submit / approve / reject / pay appliquant la machine à états
- [ ] File de validation du manager (écran dédié, actions en masse)
- [ ] Événements de domaine (`ReportSubmitted`, `ReportApproved`…) publiés dans **Kafka**
- [ ] Consumer : envoi d'e-mail (Mailpit) + écriture du journal d'audit
- [ ] Pattern **Transactional Outbox** (l'événement est écrit en base dans la même transaction, puis publié) — c'est une excellente question d'entretien
- [ ] Idempotence du consumer, gestion des erreurs, dead-letter topic
- [ ] Angular : historique d'une note (timeline), notifications en temps réel via **SSE**
- [ ] Gestion d'état avec NgRx SignalStore (file de validation, compteurs)

**Concepts :** événements de domaine, at-least-once, idempotence, outbox, RxJS (`switchMap`, `debounceTime`, `combineLatest`, `takeUntilDestroyed`).

### Jalon 5 — Comptabilité et traitements planifiés (semaine 9)

- [ ] Paiement par lots (`Reimbursement` groupés dans un `batchId`)
- [ ] Export CSV et PDF mensuel
- [ ] `@Scheduled` : relance des notes en attente depuis plus de 5 jours ; verrou distribué (ShedLock) pour ne pas exécuter le job deux fois avec deux instances
- [ ] Écran admin des politiques de dépenses
- [ ] Tableau de bord : dépenses par catégorie/département/mois (requêtes d'agrégation JPQL ou SQL natif, projections)

### Jalon 6 — Qualité, observabilité, Kubernetes (semaines 10-12)

- [ ] Tests d'intégration avec **Testcontainers** (PostgreSQL, Kafka, Keycloak)
- [ ] Tests de controllers (`@WebMvcTest` + `MockMvc`) et tests de sécurité (`@WithMockUser`, JWT de test)
- [ ] **ArchUnit** : le package `domain` ne dépend ni de Spring ni de JPA
- [ ] Tests Angular (composants, services avec `HttpTestingController`) + E2E Playwright
- [ ] SonarCloud + couverture JaCoCo
- [ ] Actuator + Micrometer + Prometheus ; logs JSON avec identifiant de corrélation
- [ ] Dockerfiles multi-stage (ou Buildpacks via `spring-boot:build-image`) ; image Angular servie par Nginx
- [ ] **Kubernetes** : chart Helm (Deployment, Service, Ingress, ConfigMap, Secret, probes liveness/readiness, ressources), testé sur k3d
- [ ] Déploiement de la démo (VPS avec k3s, ou Docker Compose si plus simple)

---

## 8. Stratégie de tests

| Niveau | Outil | Quoi |
|---|---|---|
| Domaine | JUnit 5 + AssertJ | Machine à états, `Money`, plafonds, règles de validation |
| Use cases | JUnit + Mockito | Orchestration, autorisations |
| Persistance | `@DataJpaTest` + Testcontainers | Requêtes, specifications, verrou optimiste |
| Web | `@WebMvcTest` + MockMvc | Validation, codes HTTP, sécurité |
| Intégration complète | `@SpringBootTest` + Testcontainers | Soumission → Kafka → audit |
| Architecture | ArchUnit | Dépendances hexagonales |
| Front | Jest/Vitest + Testing Library | Formulaire, calcul des totaux, guards |
| E2E | Playwright | Employé soumet → manager valide → comptable rembourse |

---

## 9. CI/CD (GitHub Actions)

```
pull_request:
  api : mvn verify (unit + intégration Testcontainers) → JaCoCo → Sonar
  web : lint → test → build
  e2e : docker compose up → Playwright
push tag v*:
  build images → push GHCR → helm upgrade (ou déploiement compose) → smoke test
```

---

## 10. Checklist « 80 % de la stack » — à cocher

**Java**
- [ ] POO, interfaces, classes abstraites, `equals`/`hashCode`
- [ ] Collections (`List`, `Set`, `Map`), génériques
- [ ] Streams, lambdas, `Optional`
- [ ] Records, sealed interfaces, pattern matching, `switch` expressions
- [ ] Exceptions contrôlées/non contrôlées
- [ ] `BigDecimal`, `java.time`
- [ ] Notions de concurrence (threads virtuels, `CompletableFuture`)

**Spring Boot**
- [ ] IoC/DI, beans, profils, `@ConfigurationProperties`
- [ ] Spring MVC : controllers, validation, gestion d'erreurs
- [ ] Spring Data JPA : repositories, requêtes dérivées, JPQL, specifications, pagination
- [ ] Hibernate : relations, lazy/eager, N+1, cache de premier niveau
- [ ] `@Transactional` (propagation, rollback, pièges de l'auto-invocation)
- [ ] Spring Security : filtres, OAuth2 resource server, sécurité par méthode
- [ ] Kafka, `@Scheduled`, événements applicatifs
- [ ] Actuator, tests Spring

**Angular**
- [ ] Composants standalone, templates, nouveau control flow (`@if`, `@for`)
- [ ] Signals, `computed`, `effect`
- [ ] RxJS : Observables, opérateurs courants, désabonnement
- [ ] Services et injection de dépendances
- [ ] Routing, lazy loading, guards, resolvers
- [ ] Formulaires réactifs, validateurs personnalisés et asynchrones
- [ ] `HttpClient`, intercepteurs
- [ ] Angular Material, `OnPush`
- [ ] Tests de composants et de services

**Transverse**
- [ ] SQL et PostgreSQL, Flyway
- [ ] OAuth2/OIDC
- [ ] Docker, Kubernetes, Helm, CI/CD
- [ ] Architecture hexagonale, DDD léger

---

## 11. Ce que vous saurez expliquer en entretien

- Le problème N+1 : comment vous l'avez détecté et corrigé.
- Pourquoi `@Transactional` ne marche pas sur un appel interne à la même classe.
- La différence entre verrou optimiste et pessimiste, et pourquoi vous avez choisi l'optimiste.
- Le pattern outbox : quel problème il résout (écriture base + publication Kafka non atomiques).
- Comment fonctionne le flow Authorization Code + PKCE.
- Pourquoi le domaine ne dépend pas de Spring, et comment ArchUnit le garantit.
- Signals vs RxJS en Angular : quand utiliser l'un ou l'autre.
- Pourquoi `BigDecimal` et pas `double` pour de l'argent.

---

## 12. README du dépôt — à inclure

1. Capture/GIF du parcours employé → manager → comptable, lien vers la démo
2. Comptes Keycloak de démonstration pour chaque rôle
3. Schéma d'architecture hexagonale + schéma de la machine à états
4. `docker compose up` → tout fonctionne (Keycloak pré-configuré)
5. Choix techniques justifiés (outbox, verrou optimiste, séparation domaine/JPA)
6. Badges CI, couverture, Quality Gate Sonar
7. Limites connues et pistes d'amélioration
