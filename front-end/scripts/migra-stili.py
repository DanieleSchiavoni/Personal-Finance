#!/usr/bin/env python3
"""
Migrazione degli stili: toglie gli stili incorporati dai componenti Angular e collega il tema scuro.

Si esegue dalla cartella front-end:   python3 scripts/migra-stili.py

Cosa fa (ogni modifica è controllata e si può rilanciare senza danni: non rifà ciò che è già fatto):
  1. copia di sicurezza della cartella src (fuori da front-end);
  2. sostituisce gli attributi style="..." con classi di styles.css;
  3. sostituisce [style.background-color], [style.width.%] e [style.height.px] con variabili CSS
     (--colore, --percentuale, --altezza): la regola vera sta nel foglio di stile;
  4. elimina i blocchi styles: [...] dei componenti (le regole sono ora in styles.css);
  5. inserisce il pulsante del tema nella barra laterale, nella barra mobile e nella home;
  6. collega i grafici ai colori del tema;
  7. aggiunge tema-scuro.css ad angular.json e lo script anti-lampeggio a index.html.
Alla fine stampa un riepilogo e segnala tutto ciò che non ha trovato o non ha saputo trasformare.
"""
import json
import re
import shutil
import sys
from pathlib import Path

RADICE = Path(__file__).resolve().parent.parent
APP = RADICE / "src" / "app"
BACKUP = RADICE.parent / "backup-src-frontend-prima-dello-stile"

fatto = []      # (file, cosa)
problemi = []   # (file, cosa)

# ---------- 2-3. Stili nei template ----------

STILI_STATICI = {
    "max-width: 420px": "larghezza-max-420",
    "max-width: 480px": "larghezza-max-480",
    "max-width: 640px": "larghezza-max-640",
    "max-width: 760px": "larghezza-max-760",
    "max-width: 960px": "larghezza-max-960",
    "width: auto": "w-auto",
    "width: 0.65rem; height: 0.65rem": "pallino",
    "width: 0.85rem; height: 0.85rem": "pallino pallino-medio",
    "height: 160px; object-fit: cover": "immagine-notizia",
    "min-width: 8.5rem": "selettore-mese-etichetta",
    "min-width: 160px": "colonna-utilizzo",
    "height: 10px": "progress-alto",
    "height: 8px": "progress-medio",
    "height: 6px": "progress-sottile",
}

# Un tag di apertura HTML, anche su più righe; gli attributi tra virgolette possono contenere ">"
TAG = re.compile(r"<[A-Za-z][\w-]*(?:\"[^\"]*\"|'[^']*'|[^<>\"'])*>")


def aggiungi_classi(tag: str, classi: str) -> str:
    nuove = classi.split()
    m = re.search(r'(\sclass=")([^"]*)(")', tag)
    if m:
        presenti = m.group(2).split()
        da_aggiungere = [c for c in nuove if c not in presenti]
        if not da_aggiungere:
            return tag
        testo = " ".join(presenti + da_aggiungere)
        return tag[: m.start()] + m.group(1) + testo + m.group(3) + tag[m.end():]
    nome = re.match(r"<[A-Za-z][\w-]*", tag)
    return tag[: nome.end()] + f' class="{" ".join(nuove)}"' + tag[nome.end():]


def trasforma_tag(tag: str, file: str) -> str:
    originale = tag

    # Stile statico -> classi
    m = re.search(r'\s+style="([^"]*)"', tag)
    if m:
        css = " ".join(m.group(1).replace("\n", " ").split()).rstrip(";").strip()
        classi = STILI_STATICI.get(css)
        if classi is None:
            problemi.append((file, f'stile non previsto, da spostare a mano: style="{m.group(1)}"'))
        else:
            tag = tag[: m.start()] + tag[m.end():]
            tag = aggiungi_classi(tag, classi)

    # Colore preso dai dati -> variabile --colore (il grigio di riserva ora è nel CSS)
    tag = re.sub(
        r'\[style\.background-color\]="([^"]*?)\s*\?\?\s*\'#6c757d\'"',
        r'[style.--colore]="\1"',
        tag,
    )

    # Percentuale presa dai dati -> variabile --percentuale
    tag = re.sub(r'\[style\.width\.%\]="([^"]*)"', r'[style.--percentuale]="\1"', tag)

    # Altezza presa dai dati -> variabile --altezza
    if re.search(r'\[style\.height\.px\]="([^"]*)"', tag):
        tag = re.sub(r'\[style\.height\.px\]="([^"]*)"', r"""[style.--altezza]="\1 + 'px'" """.rstrip(), tag)
        tag = aggiungi_classi(tag, "altezza-variabile")

    if tag != originale and re.search(r'\[style\.(background-color|width|height)', tag):
        problemi.append((file, "resta un [style.*] non previsto: " + " ".join(tag.split())[:120]))
    return tag


def migra_template(testo: str, file: str) -> str:
    def sostituisci(m: re.Match) -> str:
        tag = m.group(0)
        if "style" not in tag:
            return tag
        return trasforma_tag(tag, file)

    return TAG.sub(sostituisci, testo)


# ---------- 4. Blocchi styles: [...] ----------

BLOCCO_STYLES = re.compile(r"\n[ \t]*styles:\s*\[\s*`[^`]*`\s*,?\s*\],?")


# ---------- Modifiche mirate ----------

def ultimo_import_fine(testo: str) -> int:
    fine = 0
    for m in re.finditer(r"^import[\s\S]*?from\s+'[^']+';[ \t]*$", testo, re.M):
        fine = m.end()
    return fine


def aggiungi_import(testo: str, riga: str) -> str:
    if riga in testo:
        return testo
    pos = ultimo_import_fine(testo)
    return testo[:pos] + "\n" + riga + testo[pos:]


def sostituisci_una_volta(testo: str, vecchio: str, nuovo: str, file: str, cosa: str) -> str:
    if vecchio not in testo:
        problemi.append((file, f"punto non trovato per: {cosa}"))
        return testo
    fatto.append((file, cosa))
    return testo.replace(vecchio, nuovo, 1)


def modifica_shell(testo: str, file: str) -> str:
    if "app-pulsante-tema" in testo:
        return testo
    testo = sostituisci_una_volta(
        testo,
        "imports: [ReactiveFormsModule, RouterLink, RouterLinkActive, RouterOutlet],",
        "imports: [PulsanteTemaComponent, ReactiveFormsModule, RouterLink, RouterLinkActive, RouterOutlet],",
        file, "pulsante del tema negli imports",
    )
    testo = aggiungi_import(
        testo, "import { PulsanteTemaComponent } from '../../../shared/componenti/pulsante-tema/pulsante-tema.component';"
    )
    nuovo, n = re.subn(
        r'(<span class="fw-semibold">Personal Finance</span>)(\s*)(</header>)',
        r'\1\n        <app-pulsante-tema class="ms-auto" />\2\3',
        testo, count=1,
    )
    if n:
        fatto.append((file, "pulsante del tema nella barra mobile"))
        testo = nuovo
    else:
        problemi.append((file, "barra mobile non trovata: aggiungi <app-pulsante-tema class=\"ms-auto\" /> nell'intestazione"))
    nuovo, n = re.subn(
        r'(\s*)(<button type="button" class="btn btn-outline-secondary btn-sm w-100" \(click\)="auth\.esci\(\)">)',
        r'\1<app-pulsante-tema class="d-block mb-2" [conEtichetta]="true" />\1\2',
        testo, count=1,
    )
    if n:
        fatto.append((file, "pulsante del tema nella barra laterale"))
        testo = nuovo
    else:
        problemi.append((file, "pulsante Esci non trovato: aggiungi <app-pulsante-tema [conEtichetta]=\"true\" /> sopra"))
    return testo


def modifica_home(testo: str, file: str) -> str:
    if "app-pulsante-tema" in testo:
        return testo
    testo = sostituisci_una_volta(
        testo,
        "imports: [RouterLink, ElencoNotizieComponent],",
        "imports: [PulsanteTemaComponent, RouterLink, ElencoNotizieComponent],",
        file, "pulsante del tema negli imports",
    )
    testo = aggiungi_import(
        testo, "import { PulsanteTemaComponent } from '../../shared/componenti/pulsante-tema/pulsante-tema.component';"
    )
    nuovo, n = re.subn(
        r'(<div class="d-flex gap-2">)(\s*)(<a routerLink="/login" class="btn btn-outline-primary btn-sm">)',
        r"\1\2<app-pulsante-tema />\2\3",
        testo, count=1,
    )
    if n:
        fatto.append((file, "pulsante del tema nell'intestazione"))
        testo = nuovo
    else:
        problemi.append((file, "pulsanti Accedi/Registrati non trovati: aggiungi <app-pulsante-tema /> accanto"))
    return testo


def modifica_grafico(testo: str, file: str, con_superficie: bool) -> str:
    if "opzioniTema" in testo:
        return testo
    importa = "COLORI_GRAFICI, coloreSuperficie, opzioniTema" if con_superficie else "COLORI_GRAFICI, opzioniTema"
    testo = sostituisci_una_volta(
        testo,
        "import { COLORI_GRAFICI } from '../../../shared/utilita/colori-grafici';",
        "import { " + importa + " } from '../../../shared/utilita/colori-grafici';\n"
        "import { TemaService } from '../../../core/services/tema.service';",
        file, "import dei colori del tema",
    )
    testo = sostituisci_una_volta(
        testo,
        "private readonly spaziLavoro = inject(SpazioLavoroService);",
        "private readonly spaziLavoro = inject(SpazioLavoroService);\n  private readonly tema = inject(TemaService);",
        file, "iniezione di TemaService",
    )
    testo, n = re.subn(
        r"return \{\n(\s+)(responsive: true,|indexAxis: 'y',)",
        r"return {\n\1...opzioniTema(this.tema.scuro()),\n\1\2",
        testo,
    )
    if n:
        fatto.append((file, f"colori del tema nelle opzioni del grafico ({n})"))
    else:
        problemi.append((file, "opzioni del grafico non trovate"))
    if con_superficie:
        testo = sostituisci_una_volta(
            testo,
            "borderWidth: 2,",
            "borderWidth: 2,\n        borderColor: coloreSuperficie(this.tema.scuro()),",
            file, "bordo delle fette nel colore della scheda",
        )
        testo = sostituisci_una_volta(
            testo,
            'class="contenitore-grafico"',
            'class="contenitore-grafico contenitore-grafico-alto"',
            file, "altezza del contenitore della ciambella",
        )
    return testo


def modifica_index_html():
    percorso = RADICE / "src" / "index.html"
    testo = percorso.read_text(encoding="utf-8")
    if "pf_tema" in testo:
        return
    script = (
        "  <script>\n"
        "    // Applica subito il tema scelto, prima che Angular parta, per evitare il lampeggio\n"
        "    (function () {\n"
        "      try {\n"
        "        var scelta = localStorage.getItem('pf_tema');\n"
        "        var scuro = scelta ? scelta === 'scuro' : window.matchMedia('(prefers-color-scheme: dark)').matches;\n"
        "        document.documentElement.setAttribute('data-bs-theme', scuro ? 'dark' : 'light');\n"
        "      } catch (e) {}\n"
        "    })();\n"
        "  </script>\n"
    )
    if "</head>" not in testo:
        problemi.append(("src/index.html", "</head> non trovato: aggiungi a mano lo script del tema"))
        return
    percorso.write_text(testo.replace("</head>", script + "</head>", 1), encoding="utf-8")
    fatto.append(("src/index.html", "script anti-lampeggio del tema"))


def modifica_angular_json():
    percorso = RADICE / "angular.json"
    dati = json.loads(percorso.read_text(encoding="utf-8"))
    cambiato = False
    for progetto in dati.get("projects", {}).values():
        for obiettivo in ("build", "test"):
            opzioni = progetto.get("architect", {}).get(obiettivo, {}).get("options")
            if not opzioni or "styles" not in opzioni:
                continue
            if "src/tema-scuro.css" not in opzioni["styles"]:
                opzioni["styles"].append("src/tema-scuro.css")
                cambiato = True
    if cambiato:
        percorso.write_text(json.dumps(dati, indent=2) + "\n", encoding="utf-8")
        fatto.append(("angular.json", "tema-scuro.css aggiunto agli stili di build e test"))


def main():
    if not APP.is_dir():
        sys.exit("Esegui lo script dalla cartella front-end (non trovo src/app).")
    if not (RADICE / "src" / "tema-scuro.css").exists() or not (RADICE / "src" / "app" / "core" / "services" / "tema.service.ts").exists():
        sys.exit("Prima crea i file nuovi (tema-scuro.css, tema.service.ts, pulsante-tema...): vedi le istruzioni.")

    if not BACKUP.exists():
        shutil.copytree(RADICE / "src", BACKUP)
        print(f"Copia di sicurezza creata in: {BACKUP}")
    else:
        print(f"Copia di sicurezza già presente: {BACKUP}")

    for percorso in sorted(APP.rglob("*.ts")):
        if percorso.name.endswith(".spec.ts"):
            continue
        relativo = str(percorso.relative_to(RADICE))
        originale = percorso.read_text(encoding="utf-8")
        testo = migra_template(originale, relativo)
        testo, n_blocchi = BLOCCO_STYLES.subn("", testo)

        if percorso.name == "shell.component.ts":
            testo = modifica_shell(testo, relativo)
        elif percorso.name == "home.component.ts":
            testo = modifica_home(testo, relativo)
        elif percorso.name in ("grafici-andamento.component.ts", "grafico-budget.component.ts"):
            testo = modifica_grafico(testo, relativo, con_superficie=False)
        elif percorso.name == "grafico-spese-categoria.component.ts":
            testo = modifica_grafico(testo, relativo, con_superficie=True)

        if testo != originale:
            percorso.write_text(testo, encoding="utf-8")
            descr = []
            if n_blocchi:
                descr.append(f"{n_blocchi} blocco/i styles rimosso/i")
            n_stili = len(re.findall(r"style", originale)) - len(re.findall(r"style", testo))
            if n_stili > 0:
                descr.append("stili del template spostati nel CSS")
            fatto.append((relativo, ", ".join(descr) or "aggiornato"))

    modifica_index_html()
    modifica_angular_json()

    print("\nMODIFICHE")
    for file, cosa in fatto:
        print(f"  {file}: {cosa}")
    if not fatto:
        print("  nessuna (era già tutto a posto)")
    if problemi:
        print("\nDA CONTROLLARE A MANO")
        for file, cosa in problemi:
            print(f"  {file}: {cosa}")
        sys.exit(1)
    print("\nFinito senza problemi. Ora esegui ./scripts/verifica-stili.sh e ng build.")


if __name__ == "__main__":
    main()
