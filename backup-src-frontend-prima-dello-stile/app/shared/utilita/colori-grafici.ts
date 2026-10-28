/** Legge un colore dalle variabili CSS definite in styles.css (e ridefinite da tema-scuro.css) */
function colore(variabile: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(variabile).trim();
}

/**
 * I colori dei grafici: gli stessi dell'interfaccia, letti ogni volta dal foglio di stile,
 * che resta l'unico posto in cui i colori sono scritti.
 */
export const COLORI_GRAFICI = {
  get entrate(): string {
    return colore('--pf-entrate');
  },
  get uscite(): string {
    return colore('--pf-uscite');
  },
  get saldo(): string {
    return colore('--pf-saldo');
  },
  get saldoSfondo(): string {
    return colore('--pf-saldo-sfondo');
  },
  get attenzione(): string {
    return colore('--pf-attenzione');
  },
  get neutro(): string {
    return colore('--pf-neutro');
  },
};

/**
 * Colori di testo e griglia dei grafici per il tema in uso. Il parametro non serve al calcolo:
 * passarlo (this.tema.scuro()) fa ricalcolare le opzioni del grafico quando l'utente cambia tema.
 */
export function opzioniTema(_scuro: boolean): { color: string; borderColor: string } {
  return {
    color: colore('--pf-testo-secondario'),
    borderColor: colore('--pf-bordo'),
  };
}

/** Il colore dello sfondo delle schede: serve a staccare le fette della ciambella tra loro */
export function coloreSuperficie(_scuro: boolean): string {
  return colore('--pf-superficie');
}
