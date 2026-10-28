#!/usr/bin/env python3
"""
Carica un account demo con dati di esempio, per provare l'applicazione con grafici e schede gia pieni.

Prima di eseguirlo: avvia il backend (porta 8081) con un database vuoto o di prova.
Uso:  python3 docs/carica-dati-demo.py

Crea l'utente "demo" con:
  - lo spazio personale, con 6 mesi di movimenti, budget del mese e due obiettivi di risparmio;
  - uno spazio aziendale "Acme S.r.l." con ricavi, costi, budget, due dipendenti e gli stipendi pagati.
"""
import json
import random
import sys
import urllib.error
import urllib.request
from datetime import date

BASE = "http://localhost:8081/api"
USERNAME = "demo"
EMAIL = "demo@example.com"
PASSWORD = "Demo12345"

random.seed(7)  # gli stessi dati a ogni esecuzione
OGGI = date.today()


class ErroreApi(Exception):
    def __init__(self, stato, percorso, corpo):
        super().__init__(f"Errore {stato} su {percorso}: {corpo}")
        self.stato = stato


def chiama(metodo, percorso, corpo=None, token=None):
    dati = json.dumps(corpo).encode() if corpo is not None else None
    richiesta = urllib.request.Request(BASE + percorso, data=dati, method=metodo)
    richiesta.add_header("Content-Type", "application/json")
    if token:
        richiesta.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(richiesta) as risposta:
            testo = risposta.read().decode()
            return json.loads(testo) if testo else None
    except urllib.error.HTTPError as errore:
        raise ErroreApi(errore.code, percorso, errore.read().decode()) from None
    except urllib.error.URLError:
        sys.exit("Backend non raggiungibile su " + BASE + ": avvialo e riprova.")


def primo_del_mese(mesi_indietro):
    anno, mese = OGGI.year, OGGI.month - mesi_indietro
    while mese <= 0:
        mese += 12
        anno -= 1
    return date(anno, mese, 1)


def data_del_mese(inizio, giorno):
    """Un giorno del mese, ma mai nel futuro se il mese e quello corrente"""
    ultimo = OGGI.day if (inizio.year, inizio.month) == (OGGI.year, OGGI.month) else 28
    return inizio.replace(day=min(giorno, ultimo))


class Spazio:
    """Uno spazio di lavoro su cui inserire i dati di esempio"""

    def __init__(self, token, spazio_id):
        self.token = token
        self.id = spazio_id
        elenco = chiama("GET", f"/spazi-lavoro/{spazio_id}/categorie", token=token)
        self.categorie = {(c["nome"], c["tipo"]): c["id"] for c in elenco}

    def transazione(self, tipo, categoria, descrizione, importo, giorno, metodo="CARTA"):
        chiama("POST", f"/spazi-lavoro/{self.id}/transazioni", {
            "importo": importo,
            "descrizione": descrizione,
            "data": giorno.isoformat(),
            "tipo": tipo,
            "metodoPagamento": metodo,
            "categoriaId": self.categorie[(categoria, tipo)],
            "note": None,
        }, self.token)

    def budget(self, categoria, importo):
        chiama("POST", f"/spazi-lavoro/{self.id}/budget", {
            "categoriaId": self.categorie[(categoria, "USCITA")],
            "importo": importo,
            "mese": OGGI.month,
            "anno": OGGI.year,
        }, self.token)

    def obiettivo(self, nome, importo, versato, scadenza):
        creato = chiama("POST", f"/spazi-lavoro/{self.id}/obiettivi", {
            "nome": nome,
            "descrizione": None,
            "importoObiettivo": importo,
            "scadenza": scadenza.isoformat(),
        }, self.token)
        if versato:
            chiama("PATCH", f"/spazi-lavoro/{self.id}/obiettivi/{creato['id']}/versamenti",
                   {"importo": versato}, self.token)


def carica_spazio_personale(token):
    spazio_id = chiama("GET", "/spazi-lavoro", token=token)[0]["id"]  # il primo e quello personale
    spazio = Spazio(token, spazio_id)

    for indietro in range(5, -1, -1):
        inizio = primo_del_mese(indietro)
        spazio.transazione("ENTRATA", "Stipendio", "Stipendio", random.randint(1750, 1950),
                           data_del_mese(inizio, 27), "BONIFICO")
        if random.random() < 0.5:
            spazio.transazione("ENTRATA", "Freelance", "Lavoro extra", random.randint(150, 400),
                               data_del_mese(inizio, 15), "BONIFICO")
        spazio.transazione("USCITA", "Casa", "Affitto", 600, data_del_mese(inizio, 5), "BONIFICO")
        spazio.transazione("USCITA", "Abbonamenti", "Abbonamenti", 15, data_del_mese(inizio, 8))
        for _ in range(random.randint(3, 5)):
            spazio.transazione("USCITA", "Alimentari", "Spesa", random.randint(25, 90),
                               data_del_mese(inizio, random.randint(1, 28)))
        for _ in range(random.randint(1, 3)):
            spazio.transazione("USCITA", "Ristoranti", "Cena fuori", random.randint(20, 60),
                               data_del_mese(inizio, random.randint(1, 28)))
        spazio.transazione("USCITA", "Trasporti", "Carburante", random.randint(40, 70),
                           data_del_mese(inizio, random.randint(1, 28)))

    spazio.budget("Alimentari", 300)
    spazio.budget("Ristoranti", 120)
    spazio.budget("Trasporti", 100)
    spazio.obiettivo("Nuova automobile", 20000, 7500, date(OGGI.year + 1, 12, 31))
    spazio.obiettivo("Vacanza estiva", 1500, 600, date(OGGI.year + 1, 6, 30))


def carica_spazio_aziendale(token):
    creato = chiama("POST", "/spazi-lavoro", {"nome": "Acme S.r.l."}, token)
    spazio = Spazio(token, creato["id"])

    for indietro in range(5, -1, -1):
        inizio = primo_del_mese(indietro)
        spazio.transazione("ENTRATA", "Vendite", "Vendite del mese", random.randint(6000, 9000),
                           data_del_mese(inizio, 20), "BONIFICO")
        spazio.transazione("ENTRATA", "Servizi", "Consulenze", random.randint(1500, 3000),
                           data_del_mese(inizio, 12), "BONIFICO")
        spazio.transazione("USCITA", "Affitto", "Affitto ufficio", 1200, data_del_mese(inizio, 3), "BONIFICO")
        for _ in range(2):
            spazio.transazione("USCITA", "Fornitori", "Fornitura materiali", random.randint(300, 900),
                               data_del_mese(inizio, random.randint(1, 28)), "BONIFICO")
        spazio.transazione("USCITA", "Marketing", "Campagna pubblicitaria", random.randint(150, 400),
                           data_del_mese(inizio, random.randint(1, 28)))

    spazio.budget("Fornitori", 1500)
    spazio.budget("Marketing", 400)

    dipendenti = [
        ("Mario", "Rossi", "Sviluppatore", 1800, "2024-03-01"),
        ("Anna", "Bianchi", "Contabile", 2100, "2023-09-01"),
    ]
    identificativi = []
    for nome, cognome, mansione, stipendio, assunzione in dipendenti:
        creato_dipendente = chiama("POST", f"/spazi-lavoro/{spazio.id}/dipendenti", {
            "nome": nome,
            "cognome": cognome,
            "mansione": mansione,
            "stipendioMensile": stipendio,
            "dataAssunzione": assunzione,
            "attivo": True,
        }, token)
        identificativi.append(creato_dipendente["id"])

    # Stipendi degli ultimi tre mesi; nel mese corrente Anna risulta ancora da pagare
    for indietro in range(2, -1, -1):
        inizio = primo_del_mese(indietro)
        for posizione, dipendente_id in enumerate(identificativi):
            if indietro == 0 and posizione == 1:
                continue
            chiama("POST", f"/spazi-lavoro/{spazio.id}/dipendenti/{dipendente_id}/pagamenti-stipendio", {
                "mese": inizio.month,
                "anno": inizio.year,
                "dataPagamento": data_del_mese(inizio, 27).isoformat(),
            }, token)


def main():
    try:
        accesso = chiama("POST", "/auth/registrazione", {
            "username": USERNAME,
            "email": EMAIL,
            "password": PASSWORD,
            "nome": "Demo",
            "cognome": "Utente",
        })
    except ErroreApi as errore:
        if errore.stato == 409:
            sys.exit("L'account demo esiste gia: i dati sono gia stati caricati. "
                     "Per ripartire elimina l'utente 'demo' (o usa un database vuoto).")
        raise

    token = accesso["accessToken"]
    carica_spazio_personale(token)
    carica_spazio_aziendale(token)

    print("Dati demo caricati.")
    print(f"  Accedi su http://localhost:4200 con  utente: {USERNAME}  password: {PASSWORD}")
    print("  Spazi: Personale e Acme S.r.l. (dal selettore in alto a sinistra)")


if __name__ == "__main__":
    main()
