export function formatMoney(n: number): string {
  return new Intl.NumberFormat("ko-KR").format(Math.round(n));
}
