// src/app/features/auth/login/login.component.spec.ts
import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AutenticazioneService } from '../../../core/services/autenticazione.service';
import { UTENTE_DI_PROVA } from '../../../testing/dati-di-prova';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let componente: LoginComponent;
  let auth: { login: jasmine.Spy };
  let router: Router;

  beforeEach(async () => {
    auth = { login: jasmine.createSpy('login') };
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), { provide: AutenticazioneService, useValue: auth }],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    fixture = TestBed.createComponent(LoginComponent);
    componente = fixture.componentInstance;
    fixture.detectChanges();
  });

  function invia(): void {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('con il modulo vuoto non invia nulla e mostra i campi da correggere', () => {
    invia();

    expect(auth.login).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelectorAll('.is-invalid').length).toBe(2);
  });

  it('con dati validi esegue il login e porta alla dashboard', () => {
    auth.login.and.returnValue(of(UTENTE_DI_PROVA));
    componente.modulo.setValue({ identificativo: 'mario', password: 'Password123' });

    invia();

    expect(auth.login).toHaveBeenCalledWith({ identificativo: 'mario', password: 'Password123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('se il login fallisce mostra il messaggio del backend e riabilita il pulsante', () => {
    auth.login.and.returnValue(
      throwError(
        () => new HttpErrorResponse({ status: 401, error: { messaggio: 'Credenziali non valide' } }),
      ),
    );
    componente.modulo.setValue({ identificativo: 'mario', password: 'sbagliata' });

    invia();

    const avviso = fixture.nativeElement.querySelector('.alert-danger') as HTMLElement;
    expect(avviso.textContent).toContain('Credenziali non valide');
    expect(componente.inCorso()).toBeFalse();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
