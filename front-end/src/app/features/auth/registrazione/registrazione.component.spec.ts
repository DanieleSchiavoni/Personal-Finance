// src/app/features/auth/registrazione/registrazione.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AutenticazioneService } from '../../../core/services/autenticazione.service';
import { UTENTE_DI_PROVA } from '../../../testing/dati-di-prova';
import { RegistrazioneComponent } from './registrazione.component';

describe('RegistrazioneComponent', () => {
  let fixture: ComponentFixture<RegistrazioneComponent>;
  let componente: RegistrazioneComponent;
  let auth: { registra: jasmine.Spy };
  let router: Router;

  const datiValidi = {
    nome: 'Mario',
    cognome: 'Rossi',
    username: 'mario',
    email: 'mario@test.it',
    password: 'Password123',
    confermaPassword: 'Password123',
  };

  beforeEach(async () => {
    auth = { registra: jasmine.createSpy('registra') };
    await TestBed.configureTestingModule({
      imports: [RegistrazioneComponent],
      providers: [provideRouter([]), { provide: AutenticazioneService, useValue: auth }],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    fixture = TestBed.createComponent(RegistrazioneComponent);
    componente = fixture.componentInstance;
    fixture.detectChanges();
  });

  function invia(): void {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('password diverse rendono il modulo non valido', () => {
    componente.modulo.setValue({ ...datiValidi, confermaPassword: 'Altra1234' });

    expect(componente.modulo.invalid).toBeTrue();
    expect(componente.modulo.hasError('nonCoincidono')).toBeTrue();
  });

  it('una password senza numeri non è valida', () => {
    componente.modulo.setValue({ ...datiValidi, password: 'SoloLettere', confermaPassword: 'SoloLettere' });

    expect(componente.modulo.controls.password.hasError('pattern')).toBeTrue();
  });

  it('invia la registrazione senza il campo di conferma e porta alla dashboard', () => {
    auth.registra.and.returnValue(of(UTENTE_DI_PROVA));
    componente.modulo.setValue(datiValidi);

    invia();

    expect(auth.registra).toHaveBeenCalledWith({
      username: 'mario',
      email: 'mario@test.it',
      password: 'Password123',
      nome: 'Mario',
      cognome: 'Rossi',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });
});
