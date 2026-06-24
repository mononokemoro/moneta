import { useEffect, useState } from "react";

import { fetchDay, type DayView, type SavingsRow } from "../api/cashbook";

import { updateTransaction } from "../api/cashbook";

import type { LedgerBook } from "../api/ledgerBook";

import type {
  CategoryTransactionRow,
  CategoryTransactionsResponse,
} from "../api/categories";

import { fetchCategoryTransactionTable } from "../api/categories";

import type { TransactionTablePreview } from "../api/transactionTable";

import { TransactionTablePreviewDialog } from "./TransactionTablePreviewDialog";

import { CompactDateInput } from "./CompactDateInput";

import { formatMoney } from "../formatMoney";

import { formatDisplayDate } from "../util/dateUtil";

import { amountToInput, formatAmountInput, parseAmount } from "../util/parseAmount";



type RowDraft = CategoryTransactionRow & {

  amountInput: string;

};



type DayPreview = {

  txId: number;

  txDate: string;

  day: DayView;

};



type Props = {

  open: boolean;

  book: LedgerBook;

  busy?: boolean;

  error?: string | null;

  data: CategoryTransactionsResponse | null;

  onClose: () => void;

  onSaved: () => Promise<void>;

};



function toDraft(row: CategoryTransactionRow): RowDraft {

  return { ...row, amountInput: amountToInput(row.amount) };

}



function rowChanged(d: RowDraft, o: CategoryTransactionRow): boolean {

  return (

    d.txDate !== o.txDate ||

    d.title !== o.title ||

    parseAmount(d.amountInput) !== o.amount ||

    d.cardName !== o.cardName ||

    d.remarks !== o.remarks

  );

}



function DayPreviewDialog({

  preview,

  busy,

  error,

  onClose,

}: {

  preview: DayPreview;

  busy: boolean;

  error: string | null;

  onClose: () => void;

}) {

  const { txId, txDate, day } = preview;



  useEffect(() => {

    const onKey = (e: KeyboardEvent) => {

      if (e.key === "Escape" && !busy) onClose();

    };

    window.addEventListener("keydown", onKey);

    return () => window.removeEventListener("keydown", onKey);

  }, [busy, onClose]);



  function rowClass(id: number) {

    return id === txId ? " cb-catmgmt__dayPreviewRow--active" : "";

  }



  function renderExpenseTable() {

    if (day.expenses.length === 0) return null;

    return (

      <div className="cb-catmgmt__dayPreviewBlock">

        <h3 className="cb-catmgmt__dayPreviewTitle">지출</h3>

        <table className="cb-table cb-table--excel cb-catmgmt__dayPreviewTable">

          <thead>

            <tr>

              <th>항목</th>

              <th>분류</th>

              <th className="cb-num">금액</th>

              <th>카드</th>

              <th>비고</th>

            </tr>

          </thead>

          <tbody>

            {day.expenses.map((r) => (

              <tr key={r.id} className={`cb-row--saved${rowClass(r.id)}`}>

                <td>{r.title}</td>

                <td>{r.category}</td>

                <td className="cb-num">{formatMoney(r.amount)}</td>

                <td>{r.cardName}</td>

                <td>{r.remarks}</td>

              </tr>

            ))}

          </tbody>

        </table>

      </div>

    );

  }



  function renderIncomeTable() {

    if (day.incomes.length === 0) return null;

    return (

      <div className="cb-catmgmt__dayPreviewBlock">

        <h3 className="cb-catmgmt__dayPreviewTitle">수입</h3>

        <table className="cb-table cb-table--excel cb-catmgmt__dayPreviewTable">

          <thead>

            <tr>

              <th>항목</th>

              <th>분류</th>

              <th className="cb-num">금액</th>

              <th>비고</th>

            </tr>

          </thead>

          <tbody>

            {day.incomes.map((r) => (

              <tr key={r.id} className={`cb-row--saved${rowClass(r.id)}`}>

                <td>{r.title}</td>

                <td>{r.category}</td>

                <td className="cb-num">{formatMoney(r.amount)}</td>

                <td>{r.remarks}</td>

              </tr>

            ))}

          </tbody>

        </table>

      </div>

    );

  }



  function renderSavingsTable() {

    if (day.savings.length === 0) return null;

    return (

      <div className="cb-catmgmt__dayPreviewBlock">

        <h3 className="cb-catmgmt__dayPreviewTitle">저축</h3>

        <table className="cb-table cb-table--excel cb-catmgmt__dayPreviewTable">

          <thead>

            <tr>

              <th>항목</th>

              <th className="cb-num">금액</th>

              <th className="cb-num">누적</th>

              <th>비고</th>

            </tr>

          </thead>

          <tbody>

            {day.savings.map((r: SavingsRow) => (

              <tr key={r.id} className={`cb-row--saved${rowClass(r.id)}`}>

                <td>{r.title}</td>

                <td className="cb-num">{formatMoney(r.amount)}</td>

                <td className="cb-num">{formatMoney(r.accumulatedAmount)}</td>

                <td>{r.remarks}</td>

              </tr>

            ))}

          </tbody>

        </table>

      </div>

    );

  }



  const hasRows =

    day.expenses.length > 0 || day.incomes.length > 0 || day.savings.length > 0;



  return (

    <div className="cb-modalBackdrop cb-modalBackdrop--nested" onClick={() => !busy && onClose()}>

      <div

        className="cb-modal cb-modal--catTxDay"

        role="dialog"

        aria-modal="true"

        aria-labelledby="cb-cat-tx-day-title"

        onClick={(e) => e.stopPropagation()}

      >

        <header className="cb-modal__hdr">

          <h2 id="cb-cat-tx-day-title" className="cb-modal__title">

            {formatDisplayDate(txDate)} · {day.bookLabel} 내역

          </h2>

        </header>

        <div className="cb-modal__body cb-catmgmt__dayPreviewBody">

          {busy && <p className="cb-modal__hint">불러오는 중…</p>}

          {error && <p className="cb-err">{error}</p>}

          {!busy && !error && !hasRows && (

            <p className="cb-modal__hint">해당 일자에 등록된 내역이 없습니다.</p>

          )}

          {!busy && !error && hasRows && (

            <>

              <p className="cb-modal__hint cb-catmgmt__txHint">

                선택한 항목이 강조 표시됩니다.

              </p>

              {renderIncomeTable()}

              {renderExpenseTable()}

              {renderSavingsTable()}

            </>

          )}

        </div>

        <footer className="cb-modal__actions">

          <button type="button" className="cb-btn cb-btn--secondary" disabled={busy} onClick={onClose}>

            닫기

          </button>

        </footer>

      </div>

    </div>

  );

}



export function CategoryTransactionsDialog({

  open,

  book,

  busy = false,

  error = null,

  data,

  onClose,

  onSaved,

}: Props) {

  const [drafts, setDrafts] = useState<RowDraft[]>([]);

  const [origRows, setOrigRows] = useState<CategoryTransactionRow[]>([]);

  const [saveErr, setSaveErr] = useState<string | null>(null);

  const [rowBusy, setRowBusy] = useState(false);

  const [dayPreview, setDayPreview] = useState<DayPreview | null>(null);

  const [dayPreviewBusy, setDayPreviewBusy] = useState(false);

  const [dayPreviewErr, setDayPreviewErr] = useState<string | null>(null);

  const [tablePreview, setTablePreview] = useState<TransactionTablePreview | null>(null);

  const [tablePreviewOpen, setTablePreviewOpen] = useState(false);

  const [tablePreviewBusy, setTablePreviewBusy] = useState(false);

  const [tablePreviewErr, setTablePreviewErr] = useState<string | null>(null);



  const isExpense = data?.categoryType === "EXPENSE";

  const disabled = busy || rowBusy || dayPreviewBusy || tablePreviewBusy;



  useEffect(() => {

    if (!open) return;

    const onKey = (e: KeyboardEvent) => {

      if (e.key === "Escape" && !disabled) {

        if (dayPreview) {

          setDayPreview(null);

          return;

        }

        if (tablePreviewOpen) {

          setTablePreviewOpen(false);

          setTablePreview(null);

          return;

        }

        onClose();

      }

    };

    window.addEventListener("keydown", onKey);

    return () => window.removeEventListener("keydown", onKey);

  }, [open, disabled, onClose, dayPreview, tablePreviewOpen]);



  useEffect(() => {

    if (!open) {

      setDayPreview(null);

      setDayPreviewErr(null);

      setTablePreview(null);

      setTablePreviewErr(null);

      setTablePreviewOpen(false);

    }

  }, [open]);



  useEffect(() => {

    if (!data) {

      setDrafts([]);

      setOrigRows([]);

      return;

    }

    setDrafts(data.items.map(toDraft));

    setOrigRows(data.items);

    setSaveErr(null);

  }, [data]);



  if (!open) return null;



  const title = data

    ? `${data.categoryName} · ${data.tier === "MAJOR" ? "대분류" : "소분류"} 내역`

    : "분류 내역";



  const totalAmount = drafts.reduce((sum, d) => sum + parseAmount(d.amountInput), 0);



  function patchRow(id: number, patch: Partial<RowDraft>) {

    setDrafts((prev) => prev.map((d) => (d.id === id ? { ...d, ...patch } : d)));

  }



  async function commitRow(d: RowDraft) {

    const orig = origRows.find((r) => r.id === d.id);

    if (!orig || !rowChanged(d, orig) || rowBusy) return;



    setRowBusy(true);

    setSaveErr(null);

    try {

      await updateTransaction(d.id, {

        txDate: d.txDate,

        title: d.title.trim() || "(미입력)",

        amount: parseAmount(d.amountInput),

        categoryId: d.categoryId ?? data?.categoryId ?? null,

        cardProductId: d.cardProductId,

        cardName: isExpense ? d.cardName : "",

        remarks: d.remarks,

      });

      await onSaved();

    } catch (e: unknown) {

      setSaveErr(e instanceof Error ? e.message : "저장 실패");

    } finally {

      setRowBusy(false);

    }

  }



  function handleRowBlur(d: RowDraft, e: React.FocusEvent<HTMLTableRowElement>) {

    if (e.currentTarget.contains(e.relatedTarget as Node)) return;

    void commitRow(d);

  }



  async function openDayPreview(d: RowDraft) {

    setDayPreviewBusy(true);

    setDayPreviewErr(null);

    try {

      const day = await fetchDay(d.txDate, book);

      setDayPreview({ txId: d.id, txDate: d.txDate, day });

    } catch (e: unknown) {

      setDayPreviewErr(e instanceof Error ? e.message : "가계부 조회 실패");

      setDayPreview(null);

    } finally {

      setDayPreviewBusy(false);

    }

  }



  async function openTablePreview() {

    if (!data) return;

    setTablePreviewOpen(true);

    setTablePreviewBusy(true);

    setTablePreviewErr(null);

    try {

      const table = await fetchCategoryTransactionTable(data.categoryId, book);

      setTablePreview({

        tableName: table.tableName,

        title: `${table.categoryName} · ${table.tableName}`,

        subtitle: `${table.count}건 · 분류 내역과 동일 집합`,

        count: table.count,

        querySql: table.querySql,

        rows: table.rows,

      });

    } catch (e: unknown) {

      setTablePreviewErr(e instanceof Error ? e.message : "테이블 조회 실패");

      setTablePreview(null);

    } finally {

      setTablePreviewBusy(false);

    }

  }



  const tableCls = isExpense

    ? "cb-table cb-table--inline cb-table--excel cb-table--expense"

    : "cb-table cb-table--inline cb-table--excel cb-table--income";



  return (

    <>

      <div className="cb-modalBackdrop" onClick={() => !disabled && onClose()}>

        <div

          className={`cb-modal cb-modal--catTx${isExpense ? " cb-modal--catTx--expense" : ""}`}

          role="dialog"

          aria-modal="true"

          aria-labelledby="cb-cat-tx-dialog-title"

          onClick={(e) => e.stopPropagation()}

        >

          <header className="cb-modal__hdr">

            <h2 id="cb-cat-tx-dialog-title" className="cb-modal__title">

              {title}

            </h2>

            {data && (

              <p className="cb-modal__hdrDate">

                {drafts.length}건 · 합계 {formatMoney(totalAmount)}

              </p>

            )}

          </header>

          <div className="cb-modal__body cb-catmgmt__txDialogBody">

            {busy && <p className="cb-modal__hint">불러오는 중…</p>}

            {(error || saveErr || dayPreviewErr || tablePreviewErr) && (

              <p className="cb-err">{error || saveErr || dayPreviewErr || tablePreviewErr}</p>

            )}

            {!busy && !error && data && drafts.length === 0 && (

              <p className="cb-modal__hint">등록된 내역이 없습니다.</p>

            )}

            {!busy && !error && data && drafts.length > 0 && (

              <>

                <div className="cb-catmgmt__txHintRow">

                  <p className="cb-modal__hint cb-catmgmt__txHint">

                    셀을 수정한 뒤 다른 칸으로 이동하면 자동 저장됩니다. 내역 버튼으로 당일 가계부 내역을,

                    테이블 조회로 cb_transaction 원본을 볼 수 있습니다.

                  </p>

                  <button

                    type="button"

                    className="cb-btn cb-btn--ghost cb-btn--xs cb-catmgmt__txTableBtn"

                    disabled={disabled}

                    onClick={() => void openTablePreview()}

                  >

                    {tablePreviewBusy ? "조회 중…" : "테이블 조회"}

                  </button>

                </div>

                <section

                  className={`cb-panel cb-panel--excel cb-catmgmt__txPanel${isExpense ? " cb-catmgmt__txPanel--expense" : ""}`}

                >

                  <div className="cb-panel__tablewrap">

                    <div className="cb-panel__tablescroll">

                      <table className={tableCls}>

                        <colgroup>

                          <col className="cb-catmgmt__txColDate" />

                          <col className="cb-catmgmt__txColType" />

                          <col className="cb-catmgmt__txColTitle" />

                          <col className="cb-catmgmt__txColAmount" />

                          {isExpense && <col className="cb-catmgmt__txColCard" />}

                          <col className="cb-catmgmt__txColRemarks" />

                          <col className="cb-catmgmt__txColAction" />

                        </colgroup>

                        <thead>

                          <tr>

                            <th>일자</th>

                            <th>구분</th>

                            <th>항목</th>

                            <th className="cb-num">금액</th>

                            {isExpense && <th>카드</th>}

                            <th>비고</th>

                            <th className="cb-catmgmt__txActionHead">내역</th>

                          </tr>

                        </thead>

                        <tbody>

                          {drafts.map((d) => (

                            <tr

                              key={d.id}

                              className="cb-row--saved"

                              onBlur={(e) => handleRowBlur(d, e)}

                            >

                              <td className="cb-catmgmt__txDateCell">

                                <CompactDateInput

                                  className="cb-cell"

                                  value={d.txDate}

                                  disabled={disabled}

                                  onChange={(iso) => patchRow(d.id, { txDate: iso })}

                                />

                              </td>

                              <td className="cb-catmgmt__txTypeCell">{d.txType}</td>

                              <td>

                                <input

                                  className="cb-cell"

                                  value={d.title}

                                  disabled={disabled}

                                  onChange={(e) => patchRow(d.id, { title: e.target.value })}

                                />

                              </td>

                              <td>

                                <input

                                  className="cb-cell cb-num"

                                  value={d.amountInput}

                                  disabled={disabled}

                                  onChange={(e) =>

                                    patchRow(d.id, {

                                      amountInput: formatAmountInput(e.target.value),

                                    })

                                  }

                                />

                              </td>

                              {isExpense && (

                                <td>

                                  <input

                                    className="cb-cell"

                                    value={d.cardName}

                                    disabled={disabled}

                                    onChange={(e) =>

                                      patchRow(d.id, {

                                        cardName: e.target.value,

                                        cardProductId: null,

                                      })

                                    }

                                  />

                                </td>

                              )}

                              <td>

                                <input

                                  className="cb-cell"

                                  value={d.remarks}

                                  disabled={disabled}

                                  onChange={(e) => patchRow(d.id, { remarks: e.target.value })}

                                />

                              </td>

                              <td className="cb-catmgmt__txActionCell">

                                <button
                                  type="button"
                                  className="cb-catmgmt__txOpenBtn"
                                  title="당일 가계부 내역 조회"
                                  aria-label={`${d.title || "항목"} 당일 내역 조회`}
                                  disabled={disabled}
                                  onMouseDown={(e) => e.preventDefault()}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    void openDayPreview(d);
                                  }}
                                >
                                  <svg
                                    className="cb-catmgmt__txOpenIcon"
                                    viewBox="0 0 16 16"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="1.5"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    aria-hidden
                                  >
                                    <path d="M2.5 4.5h11" />
                                    <path d="M2.5 8h11" />
                                    <path d="M2.5 11.5H9" />
                                    <rect x="11.5" y="10" width="2.5" height="2.5" rx="0.5" />
                                  </svg>
                                </button>

                              </td>

                            </tr>

                          ))}

                        </tbody>

                      </table>

                    </div>

                  </div>

                </section>

              </>

            )}

          </div>

          <footer className="cb-modal__actions">

            <button

              type="button"

              className="cb-btn cb-btn--secondary"

              disabled={disabled}

              onClick={onClose}

            >

              닫기

            </button>

          </footer>

        </div>

      </div>

      {dayPreview && (

        <DayPreviewDialog

          preview={dayPreview}

          busy={dayPreviewBusy}

          error={dayPreviewErr}

          onClose={() => setDayPreview(null)}

        />

      )}

      <TransactionTablePreviewDialog

        open={tablePreviewOpen}

        data={tablePreview}

        busy={tablePreviewBusy}

        error={tablePreviewErr}

        nested

        onClose={() => {

          setTablePreviewOpen(false);

          setTablePreview(null);

        }}

      />

    </>

  );

}


