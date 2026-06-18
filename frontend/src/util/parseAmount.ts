export function parseAmount(raw: string): number {
  const n = Number(raw.replace(/,/g, "").trim());
  return Number.isFinite(n) ? n : 0;
}

export function amountToInput(n: number): string {
  return n ? String(n) : "";
}
