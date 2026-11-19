# 0004 — Autorisations : rôles par `@PreAuthorize`, règles fines dans le domaine

- **Statut** : Accepté
- **Date** : 2026-11-19

## Contexte

Deux familles de règles d'accès coexistent :

- **par rôle** : « seul un `MANAGER` consulte la file de validation », « seul un `ACCOUNTANT` rembourse » ; elles ne dépendent que du jeton ;
- **fines, liées à la ressource** : « seul l'auteur modifie sa note », « seul **le manager de l'auteur** valide la note » (ADR 0003 : la relation manager vit dans la table `employee`), « pas d'auto-validation ».

Spring Security propose deux façons de vérifier les règles fines :

1. un **bean d'autorisation** appelé depuis une expression SpEL, par exemple `@PreAuthorize("@reportAuthz.isManagerOf(#id)")` (équivalent des *voters* Symfony) ;
2. une **vérification dans le domaine / le service applicatif**, après chargement de l'agrégat.

## Décision

- **Rôles : déclaratifs**, `@PreAuthorize("hasRole('…')")` sur les méthodes des **services applicatifs** (`application/service`), au même endroit que `@Transactional`. `@EnableMethodSecurity` est activé dans `config/SecurityConfig`. La chaîne de filtres HTTP se contente d'exiger un jeton valide.
- **Règles fines : dans le domaine**, sous forme de fonctions pures (`domain/identity/AccessPolicy` : `isOwner`, `isManagerOf`, `requireOwner`, `requireManagerOf`). Le service applicatif charge l'agrégat et le manager de l'auteur (table `employee`) dans sa transaction, puis appelle la politique avant toute modification. Un refus lève `ForbiddenAccessException` (motif `NOT_OWNER` ou `NOT_MANAGER_OF_AUTHOR`). L'auto-validation reste refusée par l'agrégat lui-même (`approve(approver)`, issue #12) ; `isManagerOf` l'exclut aussi par défense en profondeur.
- **Format des refus** : tout refus (rôle manquant ou règle fine) donne un **403 `ProblemDetail`** (`application/problem+json`), produit par `adapter/in/web/AccessDeniedExceptionHandler` ; les refus levés par la chaîne de filtres passent par `config/ProblemDetailAccessDeniedHandler`, qui délègue à ce même advice. Le détail d'un refus de rôle reste générique (l'expression n'est jamais divulguée).

## Alternatives envisagées

- **Bean SpEL `@reportAuthz.isManagerOf(#id)`** : déclaratif et lisible sur la signature, mais
  - l'agrégat est lu deux fois (dans le bean, puis dans le service) et la vérification a lieu hors de la transaction qui modifie la note ;
  - la règle métier « seul le manager de l'auteur valide » (règle du §5 du cahier des charges) se retrouve dans une chaîne SpEL, non vérifiée à la compilation et testable seulement avec un contexte Spring ;
  - le bean doit dépendre des ports de persistance, qui n'existent pas encore (issues #15/#16).
- **Tout dans le domaine, y compris les rôles** : on perdrait le refus précoce et déclaratif des appels hors rôle, et la lisibilité « qui peut appeler quoi » sur les services.

## Conséquences

- Les règles fines sont testées en tests unitaires purs (`AccessPolicyTest`), le câblage complet (rôle → 403, manager hors équipe → 403, format `ProblemDetail`) par `MethodSecurityTest`.
- Chaque nouveau use case doit porter un `@PreAuthorize` et, s'il agit sur une note existante, appeler `AccessPolicy` (ou une méthode de l'agrégat qui l'utilise) : à vérifier en revue, et par les tests autorisé/interdit de chaque endpoint.
- Une note d'un autre employé renvoie 403 (et non 404) : l'existence de l'identifiant est révélée, ce qui est acceptable pour des UUID non devinables. À revoir si l'on veut masquer l'existence des ressources.
- `@PreAuthorize` n'agit que via le proxy Spring : un appel interne (`this.approve(...)`) contourne le contrôle, comme `@Transactional`.
