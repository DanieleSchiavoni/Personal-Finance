#!/usr/bin/env bash
# Controlla che nei componenti non ci siano stili incorporati.
# Ammessi: solo i collegamenti a variabili CSS ([style.--colore], [style.--percentuale], [style.--altezza]).
cd "$(dirname "$0")/.." || exit 1

trovati=$(grep -rnE 'style="|styles:|styleUrl|\[ngStyle\]|<style|\[style\.[a-z]' src/app --include='*.ts' --include='*.html' \
  | grep -v '\.spec\.ts' \
  | grep -vE '\[style\.--(colore|percentuale|altezza)\]')

if [ -n "$trovati" ]; then
  echo "Trovati stili incorporati:"
  echo "$trovati"
  exit 1
fi
echo "OK: nessuno stile incorporato nei componenti."
