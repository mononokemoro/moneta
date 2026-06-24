const DATA_WINDOW_NAME = "moneta-data";

export function openDataWindow(): void {
  const url = new URL(window.location.href);
  url.searchParams.set("popup", "data");
  window.open(
    url.toString(),
    DATA_WINDOW_NAME,
    "width=1200,height=860,menubar=no,toolbar=no,location=yes,resizable=yes,scrollbars=yes",
  );
}

export function isDataPopupWindow(): boolean {
  return new URLSearchParams(window.location.search).get("popup") === "data";
}
