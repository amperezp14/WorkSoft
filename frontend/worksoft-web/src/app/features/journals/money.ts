export function normalizeMoney(value: string): string {
  const input = (value.trim() || '0').replace(',', '.');
  if (!/^[0-9]{1,16}(\.[0-9]{1,2})?$/.test(input)) throw new Error('INVALID_MONEY');
  const [whole, fraction = ''] = input.split('.');
  return BigInt(whole).toString() + '.' + fraction.padEnd(2, '0');
}
export function cents(value: string): bigint {
  if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(value)) throw new Error('INVALID_MONEY');
  const [whole, fraction = ''] = value.split('.');
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'));
}
export function formatCents(value: bigint): string {
  const negative = value < 0n;
  const amount = negative ? -value : value;
  return (
    (negative ? '− ' : '') +
    '$ ' +
    new Intl.NumberFormat('es-CO', { maximumFractionDigits: 0 }).format(amount / 100n) +
    ',' +
    (amount % 100n).toString().padStart(2, '0')
  );
}
export function formatAmount(value: string): string {
  return formatCents(cents(value));
}
export function bogotaToday(): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone: 'America/Bogota',
  }).formatToParts(new Date());
  return ['year', 'month', 'day']
    .map((type) => parts.find((p) => p.type === type)!.value)
    .join('-');
}
