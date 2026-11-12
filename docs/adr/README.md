# Décisions d'architecture (ADR)

Chaque décision structurante est consignée dans un fichier `NNNN-titre-court.md`, numéroté dans l'ordre. Un ADR n'est jamais modifié après acceptation : s'il est remplacé, on crée un nouvel ADR et on passe l'ancien au statut « Remplacé par NNNN ».

| N° | Titre | Statut |
|---|---|---|
| [0001](0001-monorepo.md) | Monorepo api / web / infra | Accepté |
| [0002](0002-minio-image-source.md) | Source des images MinIO (Chainguard, épinglées par digest) | Accepté |
| [0003](0003-identity-and-manager-relationship.md) | Identité Keycloak et relation manager côté base de données | Accepté |

## Modèle

```markdown
# NNNN — Titre

- **Statut** : Proposé | Accepté | Remplacé par NNNN
- **Date** : AAAA-MM-JJ

## Contexte

Quel problème, quelles contraintes ?

## Décision

Ce que l'on choisit.

## Alternatives envisagées

- Option B — pourquoi écartée

## Conséquences

Ce que cela implique (positif et négatif).
```
