// src/app/shared/componenti/modale-conferma/modale-conferma.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModaleConfermaComponent } from './modale-conferma.component';

describe('ModaleConfermaComponent', () => {
  let fixture: ComponentFixture<ModaleConfermaComponent>;
  let componente: ModaleConfermaComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ModaleConfermaComponent] }).compileComponents();
    fixture = TestBed.createComponent(ModaleConfermaComponent);
    componente = fixture.componentInstance;
    // L'input obbligatorio va impostato prima del primo ciclo di rilevamento delle modifiche
    fixture.componentRef.setInput('titolo', 'Elimina transazione');
    fixture.componentRef.setInput('messaggio', 'Vuoi davvero eliminarla?');
    fixture.detectChanges();
  });

  function pulsanti(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.modal-footer button')) as HTMLButtonElement[];
  }

  it('mostra titolo e messaggio', () => {
    const testo = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(testo).toContain('Elimina transazione');
    expect(testo).toContain('Vuoi davvero eliminarla?');
  });

  it('il pulsante di conferma emette "confermato"', () => {
    let conferme = 0;
    componente.confermato.subscribe(() => conferme++);

    pulsanti()[1].click();

    expect(conferme).toBe(1);
  });

  it('il pulsante Annulla emette "annullato", mentre un clic dentro la finestra non chiude', () => {
    let annullamenti = 0;
    componente.annullato.subscribe(() => annullamenti++);

    (fixture.nativeElement.querySelector('.modal-content') as HTMLElement).click();
    expect(annullamenti).toBe(0);

    pulsanti()[0].click();
    expect(annullamenti).toBe(1);
  });

  it('la conferma è disabilitata se richiesto o mentre l\'operazione è in corso', () => {
    expect(pulsanti()[1].disabled).toBeFalse();

    fixture.componentRef.setInput('confermaDisabilitata', true);
    fixture.detectChanges();
    expect(pulsanti()[1].disabled).toBeTrue();

    fixture.componentRef.setInput('confermaDisabilitata', false);
    fixture.componentRef.setInput('inCorso', true);
    fixture.detectChanges();
    expect(pulsanti()[1].disabled).toBeTrue();
  });

  it('lo stile cambia il colore del pulsante di conferma', () => {
    expect(pulsanti()[1].classList).toContain('btn-danger');

    fixture.componentRef.setInput('stile', 'primary');
    fixture.detectChanges();

    expect(pulsanti()[1].classList).toContain('btn-primary');
    expect(pulsanti()[1].classList).not.toContain('btn-danger');
  });
});
