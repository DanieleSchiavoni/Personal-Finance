// src/app/shared/utilita/valuta.ts

/** Un importo scritto all'italiana con la valuta indicata, ad esempio "1.234,50 €" */
export function formattaValuta(valore: number, valuta: string): string {
  try {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency: valuta }).format(valore);
  } catch {
    // Codice valuta non riconosciuto: si mostra comunque l'importo
    return `${valore.toFixed(2)} ${valuta}`;
  }
}
