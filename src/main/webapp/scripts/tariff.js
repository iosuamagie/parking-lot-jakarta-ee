// The same tariff as Tariff.java: 15 free minutes, then 10 bani for every started minute (6 RON/hour).
export const FREE_SECONDS = 15 * 60;
export const BANI_PER_MINUTE = 10;

export function amountBani(seconds) {
  const paid = seconds - FREE_SECONDS;
  return paid <= 0 ? 0 : Math.ceil(paid / 60) * BANI_PER_MINUTE;
}

/** Writes an amount like Tariff.format: 1030 → "10.30 RON", 123450 → "1,234.50 RON". */
export function formatRon(bani) {
  const lei = Math.trunc(bani / 100).toLocaleString('en-US');
  return `${lei}.${String(bani % 100).padStart(2, '0')} RON`;
}
