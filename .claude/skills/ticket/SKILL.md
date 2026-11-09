---
name: ticket
description: Réalise une issue GitHub de Spendly de bout en bout — l'agent implementer code, l'agent reviewer vérifie, puis commit et push sur main. Argument : un numéro d'issue, ou "next" pour la prochaine issue prête. Utiliser quand l'utilisateur demande de faire, réaliser ou enchaîner un ticket/une issue.
argument-hint: <n° issue | next>
---

# /ticket — pipeline implémentation → revue → push

Argument reçu : `$ARGUMENTS`

Tu es l'**orchestrateur**. Tu n'écris pas le code toi-même : tu délègues à l'agent `implementer`, tu fais vérifier par l'agent `reviewer`, puis tu commites et pousses. Le workflow du projet est **tout sur `main`, sans branche ni PR**.

## 1. Préparer

1. Vérifier l'état du dépôt : `git status` doit être propre et la branche courante `main`. Sinon : **stop**, expliquer à l'utilisateur.
2. `git pull --ff-only`.
3. Choisir l'issue :
   - argument `next` (ou vide) : `python3 .claude/skills/ticket/next_issue.py` → numéro. S'il ne renvoie rien : plus aucune issue prête → **stop** (et arrêter la boucle si tu tournes dans `/loop`).
   - argument numérique : `python3 .claude/skills/ticket/next_issue.py --check <n>` ; si l'issue est bloquée → **stop** en listant les dépendances ouvertes.
4. `gh issue view <n>` pour connaître le titre, les labels et les tâches. Annonce en une ligne : « Ticket #n — <titre> ».
5. Si l'issue a besoin de Docker (labels `infra`, `persistence`, `messaging`, `security`, ou Testcontainers/Keycloak mentionnés), vérifier `docker info`. Docker indisponible → **stop** et demander à l'utilisateur de le démarrer.

## 2. Implémenter

Lancer l'agent `implementer` (outil Agent, `subagent_type: "implementer"`) avec un prompt qui contient : le numéro d'issue, son titre, et la consigne « Réalise l'issue #n en suivant tes instructions. Ne commite pas. »

Lire son compte rendu :
- `STATUT : DÉCISION REQUISE` → **stop**. Présenter la question et les options à l'utilisateur (outil AskUserQuestion si disponible). Une fois la réponse obtenue, relancer l'implementer avec la décision. Si la décision a une portée durable, l'ajouter à `CLAUDE.md` ou en ADR dans `docs/adr/`.
- `STATUT : BLOQUÉ` → **stop**, expliquer pourquoi à l'utilisateur.

## 3. Revue

Lancer l'agent `reviewer` (`subagent_type: "reviewer"`) avec : « Vérifie la réalisation de l'issue #n. Les changements ne sont pas commités. » Ne lui transmets **pas** le compte rendu de l'implementer : il doit vérifier de façon indépendante.

- `VERDICT : OK` → étape 4.
- `VERDICT : KO` → relancer l'`implementer` en lui transmettant **intégralement** la liste des problèmes bloquants, puis relancer le `reviewer`. **Au maximum 2 cycles de correction.** Au-delà → **stop** : laisser les changements non commités et présenter à l'utilisateur les problèmes restants.

## 4. Commit et push

1. `git status` : vérifier qu'aucun fichier sensible (`.env`, clés, `target/`, `node_modules/`) n'est inclus.
2. `git add -A` puis commit en Conventional Commits (anglais), en reprenant le type suggéré par l'implementer :

   ```
   <type>(<scope>): <description>

   <2-4 lignes résumant les changements>

   Closes #<n>

   Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
   ```
3. `git push origin main`. En cas d'échec (rejet, réseau) : **stop**, ne jamais forcer.
4. Cocher les tâches de l'issue : `gh issue view <n> --json body -q .body | sed 's/^- \[ \]/- [x]/' | gh issue edit <n> --body-file -`.
5. Commenter l'issue (`gh issue comment <n> --body-file -`) en français :
   - **Réalisé** : résumé des changements ;
   - **Vérifications** : commandes du reviewer et résultats ;
   - **À retenir** : les notes pédagogiques de l'implementer ;
   - **Remarques non bloquantes** du reviewer, s'il y en a.

## 5. Bilan

Répondre à l'utilisateur en quelques lignes : ticket réalisé, commit poussé (hash court), nombre de cycles de revue, points d'attention, et la prochaine issue prête (`next_issue.py`).

Si tu tournes dans `/loop` : continuer seulement si tout s'est bien passé ; à la moindre condition de **stop** ci-dessus, arrêter la boucle.
