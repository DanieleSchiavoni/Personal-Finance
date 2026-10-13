import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  it('si crea correttamente', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    const componente = TestBed.createComponent(AppComponent);

    expect(componente.componentInstance).toBeTruthy();
  });
});
