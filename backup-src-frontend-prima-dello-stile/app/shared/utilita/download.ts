// src/app/shared/utilita/download.ts

/** Fa scaricare al browser un file ricevuto come Blob, con il nome indicato */
export function scaricaFile(contenuto: Blob, nomeFile: string): void {
  const indirizzo = URL.createObjectURL(contenuto);
  const collegamento = document.createElement('a');
  collegamento.href = indirizzo;
  collegamento.download = nomeFile;
  document.body.appendChild(collegamento);
  collegamento.click();
  collegamento.remove();
  URL.revokeObjectURL(indirizzo);
}
