import { formatMoney } from "../formatMoney";

export function parseAmount(raw: string): number {
  const n = Number(raw.replace(/,/g, "").trim());
  return Number.isFinite(n) ? n : 0;
}

export function amountToInput(n: number): string {
  return n ? formatMoney(n) : "";
}

/** 입력 중인 금액 문자열에 천 단위 구분 쉼표를 적용합니다. */
export function formatAmountInput(raw: string): string {
  const trimmed = raw.replace(/,/g, "").trim();
  if (trimmed === "" || trimmed === "-") return trimmed;
  const n = Number(trimmed);
  if (!Number.isFinite(n)) return raw.replace(/,/g, "");
  return formatMoney(n);
}
