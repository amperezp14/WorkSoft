import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { JournalsApi, JournalValues } from './journals.api';
import { RuntimeConfigService } from '../../core/runtime-config';
describe('API de comprobantes', () => {
  let api: JournalsApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RuntimeConfigService, useValue: { config: { apiBaseUrl: '/api/v1' } } },
      ],
    });
    api = TestBed.inject(JournalsApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envía clave de reintento e importes como texto', () => {
    const values: JournalValues = {
      header: {
        date: '2026-11-16',
        type: 'GENERAL',
        origin: 'Prueba',
        description: 'Prueba',
        supportReference: 'Archivo',
        preparedBy: 'Responsable',
      },
      lines: [
        { accountId: 'cuenta', partyId: 'tercero', description: '', debit: '0.10', credit: '0.00' },
      ],
      version: 0,
    };
    api.create('empresa', values, 'same-retry-key-0001').subscribe();
    const request = http.expectOne('/api/v1/companies/empresa/journals');
    expect(request.request.headers.get('Idempotency-Key')).toBe('same-retry-key-0001');
    expect(typeof request.request.body.lines[0].debit).toBe('string');
    request.flush({});
  });
  it('contabiliza la versión leída dentro de la empresa', () => {
    api.post('empresa', 'comprobante', 2).subscribe();
    const request = http.expectOne('/api/v1/companies/empresa/journals/comprobante/post');
    expect(request.request.body).toEqual({ version: 2 });
    request.flush({});
  });
});
