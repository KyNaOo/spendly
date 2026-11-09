# 0001 — Monorepo api / web / infra

- **Statut** : Accepté
- **Date** : 2026-11-09

## Contexte

Spendly comprend une API Spring Boot, un front Angular et une infrastructure (Docker Compose, Keycloak, Helm). Le projet est développé par une seule personne et doit être facile à présenter à un recruteur.

## Décision

Un seul dépôt `spendly` avec les dossiers `api/`, `web/`, `infra/`, `docs/` et `.github/`. Les workflows CI utilisent des filtres `paths` pour ne lancer que les jobs concernés par une modification.

## Alternatives envisagées

- **Deux dépôts `spendly-api` / `spendly-web`** : versionnement indépendant, mais les changements transverses (contrat d'API + écran) se font en deux commits sur deux dépôts et la présentation est plus dispersée.

## Conséquences

- Un commit peut modifier l'API et le front de façon atomique.
- Un seul README, un seul `docker compose up` pour la démo.
- La CI doit être découpée par chemin pour rester rapide.
