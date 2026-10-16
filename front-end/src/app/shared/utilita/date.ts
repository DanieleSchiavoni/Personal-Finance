/** Una data nel formato dei campi data (aaaa-mm-gg), nel fuso orario locale */
export function formattaIso(data: Date): string {
  const mese = String(data.getMonth() + 1).padStart(2, '0');
  const giorno = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mese}-${giorno}`;
}

/** La data di oggi nel formato dei campi data (aaaa-mm-gg) */
export function oggiIso(): string {
  return formattaIso(new Date());
}

/** Da "aaaa-mm-gg" a Date, nel fuso orario locale (new Date("aaaa-mm-gg") userebbe invece l'UTC) */
export function dataDaIso(iso: string): Date {
  const [anno, mese, giorno] = iso.split('-').map(Number);
  return new Date(anno, mese - 1, giorno);
}
