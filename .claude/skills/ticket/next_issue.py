#!/usr/bin/env python3
"""Affiche le numéro de la prochaine issue ouverte dont toutes les dépendances sont fermées.

Les dépendances sont lues dans le corps de l'issue (lignes « Bloqué par #n »).
Sortie : le numéro, ou rien (code 1) s'il n'y a aucune issue prête.
Usage : python3 .claude/skills/ticket/next_issue.py [--check <n>]
  --check <n> : affiche les dépendances encore ouvertes de l'issue n (code 1 s'il y en a).
"""
import json
import re
import subprocess
import sys

DEP = re.compile(r"Bloqué par #(\d+)")


def issues(state):
    out = subprocess.run(
        ["gh", "issue", "list", "--state", state, "--limit", "500", "--json", "number,body"],
        capture_output=True, text=True, check=True,
    ).stdout
    return json.loads(out)


def main():
    open_issues = {i["number"]: i["body"] or "" for i in issues("open")}

    def blockers(n, body):
        return [int(d) for d in DEP.findall(body) if int(d) in open_issues and int(d) != n]

    if len(sys.argv) == 3 and sys.argv[1] == "--check":
        n = int(sys.argv[2])
        if n not in open_issues:
            print(f"#{n} n'est pas une issue ouverte")
            sys.exit(1)
        b = blockers(n, open_issues[n])
        if b:
            print("Bloquée par : " + ", ".join(f"#{d}" for d in b))
            sys.exit(1)
        print("prête")
        return

    for n in sorted(open_issues):
        if not blockers(n, open_issues[n]):
            print(n)
            return
    sys.exit(1)


if __name__ == "__main__":
    main()
