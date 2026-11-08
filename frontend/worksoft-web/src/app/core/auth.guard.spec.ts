import { TestBed } from '@angular/core/testing';
import {
  provideRouter,
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
} from '@angular/router';
import { signal } from '@angular/core';
import { AuthService } from './auth.service';
import { authenticatedGuard, provisionGuard } from './auth.guard';
describe('Guardas de navegación', () => {
  const auth = { authenticated: signal(false), canProvision: signal(false) };
  beforeEach(() => {
    auth.authenticated.set(false);
    auth.canProvision.set(false);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });
  it('redirige al inicio sin sesión', () => {
    const result = TestBed.runInInjectionContext(() =>
      authenticatedGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(TestBed.inject(Router).serializeUrl(result as never)).toBe('/inicio');
  });
  it('no permite el formulario a un miembro sin permiso de plataforma', () => {
    auth.authenticated.set(true);
    const result = TestBed.runInInjectionContext(() =>
      provisionGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(TestBed.inject(Router).serializeUrl(result as never)).toBe('/empresas');
  });
  it('permite el registro al operador autorizado', () => {
    auth.authenticated.set(true);
    auth.canProvision.set(true);
    expect(
      TestBed.runInInjectionContext(() =>
        provisionGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      ),
    ).toBe(true);
  });
});
