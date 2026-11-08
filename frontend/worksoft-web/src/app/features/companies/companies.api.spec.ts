import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CompaniesApi, IdempotencyKeys, normalizeCompany } from './companies.api';
import { RuntimeConfigService } from '../../core/runtime-config';
describe('CompaniesApi', () => {
  let http: HttpTestingController;
  let api: CompaniesApi;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RuntimeConfigService, useValue: { config: { apiBaseUrl: '/api/v1' } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(CompaniesApi);
  });
  afterEach(() => http.verify());
  it('envía la clave de idempotencia y conserva el NIT como texto', () => {
    api
      .create({ legalName: 'Ejemplo', nit: '0001', verificationDigit: '0' }, 'same-key-for-retries')
      .subscribe();
    const request = http.expectOne('/api/v1/companies');
    expect(request.request.headers.get('Idempotency-Key')).toBe('same-key-for-retries');
    expect(request.request.body.nit).toBe('0001');
    request.flush({});
  });
  it('envía el cursor sin modificarlo y mantiene el límite', () => {
    api.list('signed.cursor').subscribe();
    const request = http.expectOne((r) => r.url === '/api/v1/companies');
    expect(request.request.params.get('limit')).toBe('20');
    expect(request.request.params.get('cursor')).toBe('signed.cursor');
    request.flush({ items: [], nextCursor: null });
  });
});
describe('Idempotencia del formulario', () => {
  it('reutiliza la clave para datos normalizados iguales, incluso tras editar y volver', () => {
    const keys = new IdempotencyKeys();
    const a = normalizeCompany({ legalName: ' Ejemplo ', nit: '0001', verificationDigit: '0' });
    const key = keys.for(a);
    expect(keys.for(normalizeCompany({ ...a, legalName: 'Ejemplo' }))).toBe(key);
    expect(keys.for({ ...a, nit: '0002' })).not.toBe(key);
    expect(keys.for(a)).toBe(key);
  });
});
