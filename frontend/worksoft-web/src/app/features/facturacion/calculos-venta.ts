import { normalizeMoney, cents, formatCents, bogotaToday, formatAmount } from '../journals/money';
export { bogotaToday as fechaBogota, formatAmount as formatoImporte };
export function decimalNormalizado(valor: string, escala: number): string {
  const entrada = valor.trim().replace(',', '.');
  if (!new RegExp('^[0-9]{1,9}(\\.[0-9]{1,' + escala + '})?$').test(entrada))
    throw new Error('Importe inválido');
  return entrada;
}
export function importeNormalizado(valor: string): string {
  return normalizeMoney(valor);
}
function redondear(numerador: bigint, denominador: bigint) {
  return (numerador + denominador / 2n) / denominador;
}
export function calcularLinea(cantidad: string, precio: string, descuento: string, tarifa: string) {
  const normal = decimalNormalizado(cantidad, 3);
  const [entero, fraccion = ''] = normal.split('.');
  const unidades = BigInt(entero) * 1000n + BigInt(fraccion.padEnd(3, '0'));
  if (unidades <= 0n) throw new Error('La cantidad debe ser positiva');
  const porcentaje = cents(normalizeMoney(descuento)),
    ivaPorcentaje = cents(normalizeMoney(tarifa));
  if (porcentaje > 10000n || ivaPorcentaje > 10000n) throw new Error('Porcentaje inválido');
  const bruto = redondear(unidades * cents(normalizeMoney(precio)), 1000n);
  const rebaja = redondear(bruto * porcentaje, 10000n),
    base = bruto - rebaja,
    iva = redondear(base * ivaPorcentaje, 10000n);
  return { bruto, descuento: rebaja, base, iva, total: base + iva };
}
export const formatoCentavos = formatCents;
