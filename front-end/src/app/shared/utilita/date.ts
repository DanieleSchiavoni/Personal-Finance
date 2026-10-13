// src/app/shared/utilita/date.ts

/** La data di oggi nel formato dei campi data (aaaa-mm-gg), nel fuso orario locale */
export function oggiIso(): string {
  const oggi = new Date();
  const mese = String(oggi.getMonth() + 1).padStart(2, '0');
  const giorno = String(oggi.getDate()).padStart(2, '0');
  return `${oggi.getFullYear()}-${mese}-${giorno}`;
}
