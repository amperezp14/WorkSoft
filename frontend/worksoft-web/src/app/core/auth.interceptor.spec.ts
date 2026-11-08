import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { RuntimeConfigService } from './runtime-config';
describe('Protección del token', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  const auth = { accessToken: vi.fn() };
  beforeEach(() => {
    auth.accessToken.mockReset().mockResolvedValue('ephemeral-token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: RuntimeConfigService, useValue: { config: { apiBaseUrl: '/api/v1' } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });
  afterEach(() => http.verify());
  it('adjunta el token renovado a la API configurada', async () => {
    client.get('/api/v1/companies').subscribe();
    await new Promise((resolve) => setTimeout(resolve, 0));
    const request = http.expectOne('/api/v1/companies');
    expect(request.request.headers.get('Authorization')).toBe('Bearer ephemeral-token');
    request.flush({});
  });
  it('no envía credenciales a otros orígenes ni rutas similares', () => {
    for (const url of [
      'https://other.example/api/v1/companies',
      '/api/v10/companies',
      '/config.json',
    ]) {
      client.get(url).subscribe();
      const request = http.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({});
    }
    expect(auth.accessToken).not.toHaveBeenCalled();
  });
  it('no envía la petición si falla la renovación de sesión', async () => {
    auth.accessToken.mockRejectedValue(new Error('SESSION_EXPIRED'));
    const failed = vi.fn();
    client.get('/api/v1/companies').subscribe({ error: failed });
    await new Promise((resolve) => setTimeout(resolve, 0));
    http.expectNone('/api/v1/companies');
    expect(failed).toHaveBeenCalled();
  });
});
