import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PartiesApi } from './parties.api';
import { RuntimeConfigService } from '../../core/runtime-config';
it('mantiene identificación y DV como texto al guardar terceros', () => {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: RuntimeConfigService, useValue: { config: { apiBaseUrl: '/api/v1' } } },
    ],
  });
  const api = TestBed.inject(PartiesApi),
    http = TestBed.inject(HttpTestingController);
  api
    .create('empresa', {
      documentType: 'NIT',
      documentNumber: '000123',
      verificationDigit: '0',
      name: 'Ejemplo',
      category: 'OTHER',
      email: '',
      phone: '',
      address: '',
      active: true,
      version: 0,
    })
    .subscribe();
  const request = http.expectOne('/api/v1/companies/empresa/third-parties');
  expect(request.request.body.documentNumber).toBe('000123');
  request.flush({});
  http.verify();
});
