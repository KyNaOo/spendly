# Contribuer à Spendly

## Workflow

Projet mené en solo : **tout se fait directement sur `main`**, sans branche ni pull request.

1. Choisir une issue du milestone en cours dont les dépendances (« Bloqué par #n ») sont fermées.
2. Lancer les vérifications locales avant de pousser (`./mvnw verify`, `npm run lint && npm test`).
3. Le commit qui termine une issue contient `Closes #<n>` : l'issue se ferme automatiquement au push.
4. Cocher les tâches de l'issue au fil de l'avancement.
5. Si la CI casse sur `main`, la réparer en priorité.

## Commits

Les messages de commit suivent [Conventional Commits](https://www.conventionalcommits.org/) et sont rédigés **en anglais** :

```
<type>(<scope>): <description à l'impératif>
```

| Type | Usage |
|---|---|
| `feat` | nouvelle fonctionnalité |
| `fix` | correction de bug |
| `refactor` | restructuration sans changement de comportement |
| `test` | ajout ou correction de tests |
| `docs` | documentation |
| `chore` | outillage, dépendances, configuration |
| `ci` | pipelines GitHub Actions |

Scopes : `api`, `web`, `infra`, `ci`, `docs`.

Exemples :

```
feat(api): add ExpenseReport state machine
fix(web): recompute total when a line is removed
chore(infra): import keycloak realm on startup
```

## Langues

- **Anglais** : code, noms de classes/variables, commits.
- **Français** : issues, README, documentation, ADR.

## Décisions d'architecture

Toute décision structurante (choix d'une librairie, d'un pattern, d'un compromis) fait l'objet d'un ADR dans [`docs/adr/`](docs/adr/).
