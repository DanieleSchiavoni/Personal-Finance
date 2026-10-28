import { DOCUMENT } from '@angular/common';
import { Injectable, effect, inject, signal } from '@angular/core';

const CHIAVE_TEMA = 'pf_tema';

/**
 * Tema chiaro o scuro. Si applica con l'attributo data-bs-theme sull'elemento <html>: i colori veri
 * stanno nei fogli di stile (styles.css e tema-scuro.css), qui si sceglie solo quale usare.
 *
 * Se l'utente non ha mai scelto si segue il sistema operativo (e si segue anche se cambia mentre l'app è aperta);
 * appena sceglie con il pulsante, la scelta viene ricordata.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly documento = inject(DOCUMENT);

  private readonly _scuro = signal(this.temaIniziale());
  readonly scuro = this._scuro.asReadonly();

  constructor() {
    effect(() => this.applica(this._scuro()));

    const sistema = this.documento.defaultView?.matchMedia?.('(prefers-color-scheme: dark)');
    sistema?.addEventListener?.('change', (evento) => {
      if (this.sceltaSalvata() === null) {
        this._scuro.set(evento.matches);
      }
    });
  }

  inverti(): void {
    const scuro = !this._scuro();
    this._scuro.set(scuro);
    try {
      localStorage.setItem(CHIAVE_TEMA, scuro ? 'scuro' : 'chiaro');
    } catch {
      // Archivio del browser non disponibile: il tema vale solo per questa sessione
    }
  }

  private sceltaSalvata(): 'chiaro' | 'scuro' | null {
    try {
      const valore = localStorage.getItem(CHIAVE_TEMA);
      return valore === 'chiaro' || valore === 'scuro' ? valore : null;
    } catch {
      return null;
    }
  }

  private temaIniziale(): boolean {
    const salvato = this.sceltaSalvata();
    if (salvato !== null) {
      return salvato === 'scuro';
    }
    return this.documento.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  }

  private applica(scuro: boolean): void {
    this.documento.documentElement.setAttribute('data-bs-theme', scuro ? 'dark' : 'light');
  }
}
