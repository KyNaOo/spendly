# Contribuer à Spendly

## Workflow

1. Choisir une issue du milestone en cours dont les dépendances (« Bloqué par #n ») sont fermées.
2. Créer une branche depuis `main` : `<type>/<n°issue>-<slug>`, par exemple `feat/12-expense-report-aggregate`.
3. Une issue = une branche = une pull request, qui contient `Closes #<n>`.
4. Cocher les tâches de l'issue au fil de l'avancement.
5. La CI doit être verte avant de merger (squash merge recommandé).

## Types de branches et de commits

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

- **Anglais** : code, noms de classes/variables, commits, noms de branches.
- **Français** : issues, pull requests, README, documentation, ADR.

## Décisions d'architecture

Toute décision structurante (choix d'une librairie, d'un pattern, d'un compromis) fait l'objet d'un ADR dans [`docs/adr/`](docs/adr/).
