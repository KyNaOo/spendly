# 0003 — Identité Keycloak et relation manager côté base de données

- **Statut** : Accepté
- **Date** : 2026-11-12

## Contexte

Spendly délègue l'authentification à Keycloak (realm `spendly`). Les règles d'autorisation ont besoin de deux types d'information :

- le **rôle** de l'utilisateur (`EMPLOYEE`, `MANAGER`, `ACCOUNTANT`, `ADMIN`) : accès grossier aux fonctionnalités ;
- la **relation hiérarchique** (`managerId`) et le **département** : « seul le manager de l'auteur valide la note », file de validation du manager, tableau de bord par département.

Le cahier des charges modélise `Employee (id = subject Keycloak, name, email, managerId, department)` et l'issue #15 prévoit une table `employee` avec des données de démo « cohérentes avec les utilisateurs Keycloak (ids = `sub`) ». Il restait à trancher où vit la relation manager : attributs Keycloak recopiés dans le token, ou base de données de l'application.

## Décision

- **Keycloak porte l'identité et les rôles, rien de plus.** Les rôles sont des *realm roles* présents dans le claim `realm_access.roles` du jeton d'accès. Aucun attribut `department` ni `managerId` n'est défini dans Keycloak.
- **La relation manager et le département vivent dans la table `employee`** de PostgreSQL (issue #15), dont la clé primaire est le `sub` Keycloak. C'est la seule source de vérité pour « qui est le manager de qui ».
- Pour que les deux mondes concordent sans synchronisation, les utilisateurs de démo du realm ont des **identifiants fixes** (`00000000-0000-4000-8000-00000000000N`) : le `sub` du jeton est connu d'avance et réutilisable tel quel dans les données de démo Flyway.

## Alternatives envisagées

- **Attributs utilisateur Keycloak (`managerId`, `department`) exposés dans le jeton par des mappers** : pas de table à maintenir, mais
  - la relation est figée dans le jeton jusqu'à son expiration (un changement de manager n'est pas pris en compte immédiatement) ;
  - les requêtes « notes de l'équipe du manager » ou « dépenses par département » exigent de toute façon une jointure SQL, donc une copie en base ;
  - deux sources de vérité à synchroniser, et une règle métier dépendante d'un outil d'infrastructure (le domaine doit rester testable sans Keycloak).
- **Synchronisation automatique Keycloak → base (événements d'administration, SPI)** : surdimensionné pour le projet.

## Conséquences

- Les vérifications fines (`isManagerOf`, issue #9) interrogent la base, pas le jeton : un appel de lecture supplémentaire, mais une donnée toujours à jour et des tests sans Keycloak.
- Créer un nouvel utilisateur réel demande deux étapes : le compte Keycloak, puis la ligne `employee` (provisionnement à prévoir si l'application dépasse le cadre de la démo).
- Les identifiants des utilisateurs de démo font partie du contrat entre `infra/keycloak/realm-export.json` et les données de démo : les modifier d'un côté impose de les modifier de l'autre. Le script `infra/scripts/check-keycloak-realm.sh` vérifie les `sub` attendus.
