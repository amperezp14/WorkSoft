import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorMessage, bogotaDate } from './api-error';
describe('Localización y errores', () => {
  it('no muestra al usuario mensajes arbitrarios del servidor', () => {
    const message = apiErrorMessage(
      new HttpErrorResponse({ status: 500, error: { detail: 'sensitive database detail' } }),
    );
    expect(message).not.toContain('sensitive');
  });
  it('muestra la hora de Bogotá aunque el navegador esté en otra zona', () => {
    const result = bogotaDate('2026-11-08T12:00:00Z');
    expect(result).toMatch(/7:00/);
    expect(result).not.toContain('12:00');
  });
});
