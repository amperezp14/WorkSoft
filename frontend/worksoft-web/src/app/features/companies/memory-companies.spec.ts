import { MemoryCompanies } from './memory-companies';
import { HttpErrorResponse } from '@angular/common/http';
describe('Empresas en memoria', () => {
  let store: MemoryCompanies;
  beforeEach(() => (store = new MemoryCompanies()));
  const request = { legalName: ' Ejemplo ', nit: '0001', verificationDigit: '0' };
  it('crea, lista y consulta conservando el NIT y la configuración colombiana', () => {
    const company = store.create(request, 'key');
    expect(company.nit).toBe('0001');
    expect(company.legalName).toBe('Ejemplo');
    expect(company.currency).toBe('COP');
    expect(company.timeZone).toBe('America/Bogota');
    expect(store.get(company.id)).toEqual(company);
    expect(store.list(null).items).toHaveLength(1);
  });
  it('reintenta sin duplicar y rechaza la misma clave con otros datos', () => {
    const company = store.create(request, 'key');
    expect(store.create(request, 'key').id).toBe(company.id);
    expect(() => store.create({ ...request, nit: '2' }, 'key')).toThrow(HttpErrorResponse);
    expect(store.list(null).items).toHaveLength(1);
  });
  it('rechaza NIT duplicado con otra clave', () => {
    store.create(request, 'key');
    expect(() => store.create(request, 'other')).toThrow(HttpErrorResponse);
  });
  it('pagina y rechaza cursores inválidos', () => {
    for (let i = 0; i < 21; i++) store.create({ ...request, nit: String(i) }, String(i));
    const page = store.list(null);
    expect(page.items).toHaveLength(20);
    expect(store.list(page.nextCursor).items).toHaveLength(1);
    expect(() => store.list('invalid')).toThrow(HttpErrorResponse);
  });
  it('no comparte memoria entre sesiones y no expone objetos internos', () => {
    const company = store.create(request, 'key');
    company.legalName = 'Cambio';
    expect(store.get(company.id).legalName).toBe('Ejemplo');
    expect(new MemoryCompanies().list(null).items).toHaveLength(0);
  });
});
