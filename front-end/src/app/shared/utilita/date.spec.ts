// src/app/shared/utilita/date.spec.ts
import { dataDaIso, formattaIso, oggiIso } from './date';

describe('date', () => {
  it('formattaIso scrive la data come aaaa-mm-gg, con gli zeri iniziali', () => {
    expect(formattaIso(new Date(2026, 9, 6))).toBe('2026-10-06');
    expect(formattaIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('dataDaIso legge la data nel fuso orario locale', () => {
    const data = dataDaIso('2026-10-06');

    expect(data.getFullYear()).toBe(2026);
    expect(data.getMonth()).toBe(9); // i mesi di JavaScript partono da zero
    expect(data.getDate()).toBe(6);
  });

  it('formattaIso e dataDaIso sono l\'una l\'inverso dell\'altra', () => {
    expect(formattaIso(dataDaIso('2027-12-31'))).toBe('2027-12-31');
  });

  it('oggiIso restituisce una data nel formato corretto', () => {
    expect(oggiIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
