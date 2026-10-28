// src/app/shared/componenti/selettore-mese/selettore-mese.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelettoreMeseComponent } from './selettore-mese.component';

describe('SelettoreMeseComponent', () => {
  let fixture: ComponentFixture<SelettoreMeseComponent>;
  let componente: SelettoreMeseComponent;
  const oggi = new Date();

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SelettoreMeseComponent] }).compileComponents();
    fixture = TestBed.createComponent(SelettoreMeseComponent);
    componente = fixture.componentInstance;
    fixture.componentRef.setInput('mese', 1);
    fixture.componentRef.setInput('anno', 2026);
    fixture.detectChanges();
  });

  it('da gennaio il mese precedente è dicembre dell\'anno prima', () => {
    componente.precedente();

    expect(componente.mese()).toBe(12);
    expect(componente.anno()).toBe(2025);
  });

  it('da dicembre il mese successivo è gennaio dell\'anno dopo', () => {
    fixture.componentRef.setInput('mese', 12);
    fixture.componentRef.setInput('consentiFuturo', true);

    componente.successivo();

    expect(componente.mese()).toBe(1);
    expect(componente.anno()).toBe(2027);
  });

  it('nel mese corrente non si può andare avanti, se il futuro non è consentito', () => {
    fixture.componentRef.setInput('mese', oggi.getMonth() + 1);
    fixture.componentRef.setInput('anno', oggi.getFullYear());
    fixture.detectChanges();

    const successivo = fixture.nativeElement.querySelector('button[aria-label="Mese successivo"]') as HTMLButtonElement;
    expect(componente.successivoBloccato()).toBeTrue();
    expect(successivo.disabled).toBeTrue();

    fixture.componentRef.setInput('consentiFuturo', true);
    fixture.detectChanges();

    expect(componente.successivoBloccato()).toBeFalse();
    expect(successivo.disabled).toBeFalse();
  });

  it('negli altri mesi si può sempre andare avanti', () => {
    expect(componente.successivoBloccato()).toBeFalse();
  });

  it('tornaAOggi riporta al mese corrente', () => {
    componente.tornaAOggi();

    expect(componente.mese()).toBe(oggi.getMonth() + 1);
    expect(componente.anno()).toBe(oggi.getFullYear());
  });
});
