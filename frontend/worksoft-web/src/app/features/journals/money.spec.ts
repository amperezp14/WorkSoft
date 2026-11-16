import { normalizeMoney, cents, formatAmount } from './money';
describe('Importes COP exactos', () => {
  it('normaliza coma decimal sin punto flotante', () => {
    expect(normalizeMoney('0,1')).toBe('0.10');
    expect(cents('0.10') + cents('0.20')).toBe(cents('0.30'));
  });
  it('preserva importes mayores que el entero seguro de JavaScript', () => {
    expect(normalizeMoney('9007199254740993.01')).toBe('9007199254740993.01');
    expect(formatAmount('9007199254740993.01')).toContain('9.007.199.254.740.993,01');
  });
  it('rechaza redondeo implícito, negativos y notación científica', () => {
    for (const value of ['1.001', '-1', '1e3', '1.000,00'])
      expect(() => normalizeMoney(value)).toThrow();
  });
  it('conserva los ceros como importe de dos decimales', () => {
    expect(normalizeMoney('')).toBe('0.00');
    expect(normalizeMoney('0001')).toBe('1.00');
  });
});
