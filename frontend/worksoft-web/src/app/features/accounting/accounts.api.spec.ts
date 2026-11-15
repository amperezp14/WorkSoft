import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AccountsApi } from './accounts.api';
import { RuntimeConfigService } from '../../core/runtime-config';
describe('API de cuentas por empresa', () => {
  let api: AccountsApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RuntimeConfigService, useValue: { config: { apiBaseUrl: '/api/v1' } } },
      ],
    });
    api = TestBed.inject(AccountsApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('consulta por empresa con paginación y filtro', () => {
    api.list('empresa-1', 'caja', 2).subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/companies/empresa-1/accounts');
    expect(req.request.params.get('q')).toBe('caja');
    expect(req.request.params.get('page')).toBe('2');
    req.flush({ items: [], total: 0, page: 2, size: 25 });
  });
  it('actualiza conservando código y versión para evitar sobrescrituras', () => {
    api
      .update('empresa-1', 'cuenta-1', {
        code: '11050501',
        name: 'Caja',
        nature: 'DEBIT',
        active: true,
        allowsPosting: true,
        notes: '',
        version: 3,
      })
      .subscribe();
    const req = http.expectOne('/api/v1/companies/empresa-1/accounts/cuenta-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.version).toBe(3);
    expect(req.request.body.code).toBe('11050501');
    req.flush({});
  });
});
