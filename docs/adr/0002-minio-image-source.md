# 0002 — Source des images MinIO

- **Statut** : Accepté
- **Date** : 2026-11-10

## Contexte

Les justificatifs sont stockés dans MinIO (API S3), lancé en local par `infra/docker-compose.yml` avec un job d'initialisation qui crée le bucket `receipts` grâce au client `mc`.

Les images officielles ne sont plus téléchargeables : `minio/minio` et `minio/mc` sur Docker Hub renvoient « access denied », et `quay.io/minio/*` exige une authentification. MinIO ne distribue plus son édition communautaire que sous forme de sources, et cette édition n'a plus de console d'administration complète (il reste un simple navigateur d'objets).

## Décision

Utiliser les images **Chainguard** `chainguard/minio` (serveur, qui contient aussi `mc`) et `chainguard/minio-client`, **épinglées par digest** (`image@sha256:…`). Le compose indique en commentaire le tag d'origine (`latest`) et la date de récupération.

Chainguard ne publie gratuitement que le tag `latest`. Épingler le digest rend les environnements reproductibles et empêche une mise à jour silencieuse.

## Alternatives envisagées

- **`pgsty/minio` / `pgsty/mc`** (fork communautaire Pigsty) : tags versionnés et lisibles, mais l'éditeur tiers est moins établi, d'où un risque plus fort sur la chaîne d'approvisionnement.
- **Construire l'image depuis les sources MinIO** : maîtrise totale, mais il faudrait maintenir un Dockerfile et une étape de build pour une simple dépendance de développement.
- **Remplacer MinIO** (RustFS, SeaweedFS, Garage) : le code applicatif resterait identique grâce à l'API S3, mais la stack prévue par le cahier des charges changerait et ces produits sont moins connus des recruteurs.

## Conséquences

- Les images Chainguard sont minimales (pas de `curl`, `wget` ni `grep`) : le healthcheck interroge `/minio/health/ready` en bash pur, et le job `minio-init` (sans shell) déclare l'alias `mc` par la variable `MC_HOST_local`.
- Pour mettre MinIO à jour, il faut récupérer le nouveau digest (`docker pull chainguard/minio:latest` puis `docker inspect --format '{{index .RepoDigests 0}}'`), puis modifier le compose et la date en commentaire.
- Le chart Helm (jalon 6) devra utiliser la même source d'image.
