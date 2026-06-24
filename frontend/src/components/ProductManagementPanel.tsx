import { useEffect, useState } from "react";
import type { LedgerBook } from "../api/ledgerBook";
import {
  CARD_CLASSES,
  createCardRow,
  createInsuranceRow,
  createLoanRow,
  createSavingsRow,
  INSURANCE_CLASSES,
  INSURANCE_PAYMENTS,
  loadProductStore,
  normalizeCardDay,
  normalizeYmdDate,
  saveProductStore,
  todayYmd,
  syncCardsFromTransactions,
  SAVINGS_CLASSES,
  type CardProduct,
  type CardStatus,
  type InsuranceProduct,
  type InsuranceStatus,
  type LoanProduct,
  type LoanStatus,
  type ProductStore,
  type SavingsProduct,
  type SavingsStatus,
} from "../util/productPrefs";
import { MonetaHint, MonetaPanel, MonetaSubNav } from "./MonetaPanel";

type Props = {
  book: LedgerBook;
};

type MainTab = "savings" | "insurance" | "loan" | "card";

function swapRows<T>(rows: T[], index: number, dir: -1 | 1): T[] {
  const target = index + dir;
  if (target < 0 || target >= rows.length) return rows;
  const next = [...rows];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}

function toggleCheck(set: Set<string>, id: string): Set<string> {
  const next = new Set(set);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  return next;
}

const TABLE_CLS = "cb-table cb-table--inline cb-table--excel cb-table--product";
const CARD_TABLE_CLS = `${TABLE_CLS} cb-table--card`;

function cellInput(
  value: string,
  onChange: (v: string) => void,
  opts?: { compact?: boolean; placeholder?: string; disabled?: boolean }
) {
  return (
    <input
      className={opts?.compact ? "cb-cell cb-prod__cellCompact" : "cb-cell"}
      value={value}
      placeholder={opts?.placeholder}
      disabled={opts?.disabled}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

function cellSelect(value: string, options: string[], onChange: (v: string) => void, disabled?: boolean, compact?: boolean) {
  return (
    <select
      className={compact ? "cb-cell cb-prod__cellCompact" : "cb-cell"}
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value)}
    >
      {options.map((o) => (
        <option key={o} value={o}>
          {o}
        </option>
      ))}
    </select>
  );
}

export function ProductManagementPanel({ book }: Props) {
  const [mainTab, setMainTab] = useState<MainTab>("savings");
  const [savingsSub, setSavingsSub] = useState<SavingsStatus>("active");
  const [insuranceSub, setInsuranceSub] = useState<InsuranceStatus>("active");
  const [loanSub, setLoanSub] = useState<LoanStatus>("active");
  const [cardSub, setCardSub] = useState<CardStatus>("active");
  const [store, setStore] = useState<ProductStore>({
    savings: [],
    insurance: [],
    loans: [],
    cards: [],
  });
  const [loading, setLoading] = useState(true);
  const [checked, setChecked] = useState<Set<string>>(() => new Set());
  const [selectedIdx, setSelectedIdx] = useState(0);
  const [err, setErr] = useState<string | null>(null);
  const [syncingCards, setSyncingCards] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    loadProductStore(book)
      .then((data) => {
        if (!cancelled) {
          setStore(data);
          setChecked(new Set());
          setSelectedIdx(0);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) setErr(e instanceof Error ? e.message : "조회 실패");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [book]);

  function persist(next: ProductStore) {
    setStore(next);
  }

  function currentSub() {
    switch (mainTab) {
      case "savings":
        return savingsSub;
      case "insurance":
        return insuranceSub;
      case "loan":
        return loanSub;
      case "card":
        return cardSub;
    }
  }

  function isArchiveView() {
    return currentSub() !== "active";
  }

  async function handleSave() {
    setErr(null);
    try {
      const saved = await saveProductStore(book, store);
      setStore(saved);
      window.alert("저장되었습니다.");
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "저장 실패");
    }
  }

  async function handleSyncCardsFromTransactions() {
    setErr(null);
    setSyncingCards(true);
    try {
      const { added, store: next } = await syncCardsFromTransactions(book);
      setStore(next);
      setChecked(new Set());
      setSelectedIdx(0);
      if (added === 0) {
        window.alert("지출 내역에 있는 카드명이 모두 상품관리에 등록되어 있습니다.");
      } else {
        window.alert(`${added}개 카드를 지출 내역에서 가져왔습니다.`);
      }
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "카드 가져오기 실패");
    } finally {
      setSyncingCards(false);
    }
  }

  function handleClose() {
    setErr(null);
    setChecked(new Set());
  }

  function updateSavings(id: string, patch: Partial<SavingsProduct>) {
    persist({
      ...store,
      savings: store.savings.map((r) => (r.id === id ? { ...r, ...patch } : r)),
    });
  }

  function updateInsurance(id: string, patch: Partial<InsuranceProduct>) {
    persist({
      ...store,
      insurance: store.insurance.map((r) => (r.id === id ? { ...r, ...patch } : r)),
    });
  }

  function updateLoan(id: string, patch: Partial<LoanProduct>) {
    persist({
      ...store,
      loans: store.loans.map((r) => (r.id === id ? { ...r, ...patch } : r)),
    });
  }

  function updateCard(id: string, patch: Partial<CardProduct>) {
    persist({
      ...store,
      cards: store.cards.map((r) => (r.id === id ? { ...r, ...patch } : r)),
    });
  }

  function moveStatus(target: SavingsStatus | InsuranceStatus | LoanStatus | CardStatus) {
    const ids = checked.size ? [...checked] : [];
    if (ids.length === 0) {
      setErr("이동할 항목을 선택하세요.");
      return;
    }
    if (mainTab === "savings") {
      persist({
        ...store,
        savings: store.savings.map((r) =>
          ids.includes(r.id) ? { ...r, status: target as SavingsStatus } : r
        ),
      });
    } else if (mainTab === "insurance") {
      persist({
        ...store,
        insurance: store.insurance.map((r) =>
          ids.includes(r.id) ? { ...r, status: target as InsuranceStatus } : r
        ),
      });
    } else if (mainTab === "loan") {
      persist({
        ...store,
        loans: store.loans.map((r) =>
          ids.includes(r.id) ? { ...r, status: target as LoanStatus } : r
        ),
      });
    } else {
      persist({
        ...store,
        cards: store.cards.map((r) => {
          if (!ids.includes(r.id)) return r;
          const status = target as CardStatus;
          if (status === "cancelled") {
            return { ...r, status, cancelDate: r.cancelDate || todayYmd() };
          }
          if (status === "active") {
            return { ...r, status, cancelDate: "" };
          }
          return { ...r, status };
        }),
      });
    }
    setChecked(new Set());
  }

  function deleteSelected() {
    const ids = checked.size ? [...checked] : [];
    if (ids.length === 0) {
      setErr("삭제할 항목을 선택하세요.");
      return;
    }
    if (!window.confirm(`선택한 ${ids.length}개 항목을 삭제할까요?`)) return;
    if (mainTab === "savings") {
      persist({ ...store, savings: store.savings.filter((r) => !ids.includes(r.id)) });
    } else if (mainTab === "insurance") {
      persist({ ...store, insurance: store.insurance.filter((r) => !ids.includes(r.id)) });
    } else if (mainTab === "loan") {
      persist({ ...store, loans: store.loans.filter((r) => !ids.includes(r.id)) });
    } else {
      persist({ ...store, cards: store.cards.filter((r) => !ids.includes(r.id)) });
    }
    setChecked(new Set());
    setSelectedIdx(0);
  }

  function addProduct() {
    if (mainTab === "savings") {
      persist({ ...store, savings: [...store.savings, createSavingsRow()] });
    } else if (mainTab === "insurance") {
      persist({ ...store, insurance: [...store.insurance, createInsuranceRow()] });
    } else if (mainTab === "loan") {
      persist({ ...store, loans: [...store.loans, createLoanRow()] });
    } else {
      persist({ ...store, cards: [...store.cards, createCardRow()] });
    }
  }

  function reorderSelected(dir: -1 | 1) {
    if (mainTab === "savings") {
      const filtered = store.savings.filter((r) => r.status === savingsSub);
      const idx = Math.min(selectedIdx, filtered.length - 1);
      const id = filtered[idx]?.id;
      if (!id) return;
      const allIdx = store.savings.findIndex((r) => r.id === id);
      persist({ ...store, savings: swapRows(store.savings, allIdx, dir) });
      setSelectedIdx(Math.max(0, idx + dir));
    } else if (mainTab === "insurance") {
      const filtered = store.insurance.filter((r) => r.status === insuranceSub);
      const idx = Math.min(selectedIdx, filtered.length - 1);
      const id = filtered[idx]?.id;
      if (!id) return;
      const allIdx = store.insurance.findIndex((r) => r.id === id);
      persist({ ...store, insurance: swapRows(store.insurance, allIdx, dir) });
      setSelectedIdx(Math.max(0, idx + dir));
    } else if (mainTab === "loan") {
      const filtered = store.loans.filter((r) => r.status === loanSub);
      const idx = Math.min(selectedIdx, filtered.length - 1);
      const id = filtered[idx]?.id;
      if (!id) return;
      const allIdx = store.loans.findIndex((r) => r.id === id);
      persist({ ...store, loans: swapRows(store.loans, allIdx, dir) });
      setSelectedIdx(Math.max(0, idx + dir));
    } else {
      const filtered = store.cards.filter((r) => r.status === cardSub);
      const idx = Math.min(selectedIdx, filtered.length - 1);
      const id = filtered[idx]?.id;
      if (!id) return;
      const allIdx = store.cards.findIndex((r) => r.id === id);
      persist({ ...store, cards: swapRows(store.cards, allIdx, dir) });
      setSelectedIdx(Math.max(0, idx + dir));
    }
  }

  function renderSubNav() {
    if (mainTab === "savings") {
      return (
        <MonetaSubNav
          active={savingsSub}
          onChange={(id) => {
            setSavingsSub(id as SavingsStatus);
            setChecked(new Set());
            setSelectedIdx(0);
          }}
          items={[
            { id: "active", label: "가입중인 저축" },
            { id: "matured", label: "만기된 저축" },
            { id: "terminated", label: "중도해지 저축" },
          ]}
        />
      );
    }
    if (mainTab === "insurance") {
      return (
        <MonetaSubNav
          active={insuranceSub}
          onChange={(id) => {
            setInsuranceSub(id as InsuranceStatus);
            setChecked(new Set());
            setSelectedIdx(0);
          }}
          items={[
            { id: "active", label: "납입중인 보험" },
            { id: "matured", label: "만기된 보험" },
            { id: "terminated", label: "중도해지 보험" },
          ]}
        />
      );
    }
    if (mainTab === "loan") {
      return (
        <MonetaSubNav
          active={loanSub}
          onChange={(id) => {
            setLoanSub(id as LoanStatus);
            setChecked(new Set());
            setSelectedIdx(0);
          }}
          items={[
            { id: "active", label: "상환중인 대출" },
            { id: "matured", label: "만기된 대출" },
            { id: "prepaid", label: "중도상환 대출" },
          ]}
        />
      );
    }
    return (
      <MonetaSubNav
        active={cardSub}
        onChange={(id) => {
          setCardSub(id as CardStatus);
          setChecked(new Set());
          setSelectedIdx(0);
        }}
        items={[
          { id: "active", label: "사용중인 카드" },
          { id: "cancelled", label: "해지한 카드" },
        ]}
      />
    );
  }

  function renderHint() {
    if (mainTab === "savings") {
      return (
        <MonetaHint>
          분류와 저축명은 필수 입력항목입니다. 가입일과 만기일은 20100101 형태로 입력해 주세요.
        </MonetaHint>
      );
    }
    if (mainTab === "insurance") {
      return (
        <MonetaHint>
          분류와 보험명은 필수 입력항목입니다. 가입일과 만기일은 20100101 형태로 입력해 주세요.
        </MonetaHint>
      );
    }
    if (mainTab === "loan") {
      return (
        <MonetaHint>
          대출명과 대출원금은 필수 입력항목입니다. 시작일과 만기일은 20100101 형태로 입력해 주세요.
        </MonetaHint>
      );
    }
    return (
      <MonetaHint>
        분류와 카드명은 필수 입력항목입니다. 사용 시작·해지일은 20260101 형태로 입력해 주세요. 「내역에서
        가져오기」로 지출 내역의 카드명을 일괄 등록할 수 있습니다.
      </MonetaHint>
    );
  }

  function renderSavingsTable() {
    const rows = store.savings.filter((r) => r.status === savingsSub);
    return (
      <table className={TABLE_CLS}>
        <thead>
          <tr>
            <th className="cb-col-check" />
            <th>
              분류 <span className="cb-prod__req">*</span>
            </th>
            <th>
              저축명 <span className="cb-prod__req">*</span>
            </th>
            <th>가입일</th>
            <th>만기일</th>
            <th>개설잔액</th>
            <th>기준일</th>
            <th>자동이체일</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr
              key={row.id}
              className={selectedIdx === idx ? "cb-prod__row--selected" : undefined}
              onClick={() => setSelectedIdx(idx)}
            >
              <td>
                <input
                  type="checkbox"
                  checked={checked.has(row.id)}
                  onChange={() => setChecked(toggleCheck(checked, row.id))}
                  onClick={(e) => e.stopPropagation()}
                />
              </td>
              <td>{cellSelect(row.classification, SAVINGS_CLASSES, (v) => updateSavings(row.id, { classification: v }))}</td>
              <td>{cellInput(row.name, (v) => updateSavings(row.id, { name: v }))}</td>
              <td>{cellInput(row.joinDate, (v) => updateSavings(row.id, { joinDate: v }))}</td>
              <td>{cellInput(row.maturityDate, (v) => updateSavings(row.id, { maturityDate: v }))}</td>
              <td>{cellInput(row.openingBalance, (v) => updateSavings(row.id, { openingBalance: v }))}</td>
              <td>{cellInput(row.openingDate, (v) => updateSavings(row.id, { openingDate: v }), { placeholder: "YYYYMMDD" })}</td>
              <td className="cb-prod__dayCell">
                {cellInput(row.autoTransferDay, (v) => updateSavings(row.id, { autoTransferDay: v }), { compact: true })}
                <span>일</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  function renderInsuranceTable() {
    const rows = store.insurance.filter((r) => r.status === insuranceSub);
    return (
      <table className={TABLE_CLS}>
        <thead>
          <tr>
            <th className="cb-col-check" />
            <th>
              분류 <span className="cb-prod__req">*</span>
            </th>
            <th>
              보험명 <span className="cb-prod__req">*</span>
            </th>
            <th>결제방법</th>
            <th>가입일</th>
            <th>만기일</th>
            <th>개설잔액</th>
            <th>기준일</th>
            <th>이체일</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr
              key={row.id}
              className={selectedIdx === idx ? "cb-prod__row--selected" : undefined}
              onClick={() => setSelectedIdx(idx)}
            >
              <td>
                <input
                  type="checkbox"
                  checked={checked.has(row.id)}
                  onChange={() => setChecked(toggleCheck(checked, row.id))}
                  onClick={(e) => e.stopPropagation()}
                />
              </td>
              <td>{cellSelect(row.classification, INSURANCE_CLASSES, (v) => updateInsurance(row.id, { classification: v }))}</td>
              <td>{cellInput(row.name, (v) => updateInsurance(row.id, { name: v }))}</td>
              <td>{cellSelect(row.paymentMethod, INSURANCE_PAYMENTS, (v) => updateInsurance(row.id, { paymentMethod: v }))}</td>
              <td>{cellInput(row.joinDate, (v) => updateInsurance(row.id, { joinDate: v }))}</td>
              <td>{cellInput(row.maturityDate, (v) => updateInsurance(row.id, { maturityDate: v }))}</td>
              <td>{cellInput(row.openingBalance, (v) => updateInsurance(row.id, { openingBalance: v }))}</td>
              <td>{cellInput(row.openingDate, (v) => updateInsurance(row.id, { openingDate: v }), { placeholder: "YYYYMMDD" })}</td>
              <td className="cb-prod__dayCell">
                {cellInput(row.transferDay, (v) => updateInsurance(row.id, { transferDay: v }), { compact: true })}
                <span>일</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  function renderLoanTable() {
    const rows = store.loans.filter((r) => r.status === loanSub);
    return (
      <table className={TABLE_CLS}>
        <thead>
          <tr>
            <th className="cb-col-check" />
            <th>
              대출명 <span className="cb-prod__req">*</span>
            </th>
            <th>대출원금</th>
            <th>시작일</th>
            <th>만기일</th>
            <th>매월 상환일</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr
              key={row.id}
              className={selectedIdx === idx ? "cb-prod__row--selected" : undefined}
              onClick={() => setSelectedIdx(idx)}
            >
              <td>
                <input
                  type="checkbox"
                  checked={checked.has(row.id)}
                  onChange={() => setChecked(toggleCheck(checked, row.id))}
                  onClick={(e) => e.stopPropagation()}
                />
              </td>
              <td>{cellInput(row.name, (v) => updateLoan(row.id, { name: v }))}</td>
              <td>{cellInput(row.principal, (v) => updateLoan(row.id, { principal: v }))}</td>
              <td>{cellInput(row.startDate, (v) => updateLoan(row.id, { startDate: v }))}</td>
              <td>{cellInput(row.maturityDate, (v) => updateLoan(row.id, { maturityDate: v }))}</td>
              <td className="cb-prod__dayCell">
                {cellInput(row.repaymentDay, (v) => updateLoan(row.id, { repaymentDay: v }), { compact: true })}
                <span>일</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  function renderCardTable() {
    const rows = store.cards.filter((r) => r.status === cardSub);
    return (
      <table className={CARD_TABLE_CLS}>
        <colgroup>
          <col className="cb-col-check" />
          <col className="cb-prod__col-class" />
          <col className="cb-prod__col-cardName" />
          <col className="cb-prod__col-payDay" />
          <col className="cb-prod__col-date" />
          <col className="cb-prod__col-date" />
        </colgroup>
        <thead>
          <tr>
            <th className="cb-col-check" />
            <th>
              분류 <span className="cb-prod__req">*</span>
            </th>
            <th>
              카드명 <span className="cb-prod__req">*</span>
            </th>
            <th>결제일</th>
            <th>사용 시작</th>
            <th>해지일</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => {
            const isCheck = row.classification === "체크카드";
            return (
              <tr
                key={row.id}
                className={selectedIdx === idx ? "cb-prod__row--selected" : undefined}
                onClick={() => setSelectedIdx(idx)}
              >
                <td>
                  <input
                    type="checkbox"
                    checked={checked.has(row.id)}
                    onChange={() => setChecked(toggleCheck(checked, row.id))}
                    onClick={(e) => e.stopPropagation()}
                  />
                </td>
                <td>
                  {cellSelect(row.classification, CARD_CLASSES, (v) => {
                    const patch = createCardRow(v);
                    updateCard(row.id, {
                      classification: v,
                      paymentDay: patch.paymentDay,
                    });
                  })}
                </td>
                <td>{cellInput(row.name, (v) => updateCard(row.id, { name: v }))}</td>
                <td className="cb-prod__dayCell">
                  <span className="cb-prod__inlineCell">
                    {cellInput(
                      row.paymentDay,
                      (v) => updateCard(row.id, { paymentDay: normalizeCardDay(v) }),
                      { compact: true, disabled: isCheck }
                    )}
                    <span>일</span>
                  </span>
                </td>
                <td>
                  {cellInput(
                    row.usageStartDate,
                    (v) => updateCard(row.id, { usageStartDate: normalizeYmdDate(v) }),
                    { placeholder: "YYYYMMDD" }
                  )}
                </td>
                <td>
                  {cellInput(
                    row.cancelDate,
                    (v) => updateCard(row.id, { cancelDate: normalizeYmdDate(v) }),
                    { placeholder: "YYYYMMDD", disabled: cardSub === "active" }
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    );
  }

  function renderTable() {
    switch (mainTab) {
      case "savings":
        return renderSavingsTable();
      case "insurance":
        return renderInsuranceTable();
      case "loan":
        return renderLoanTable();
      case "card":
        return renderCardTable();
    }
  }

  function renderToolbar() {
    if (isArchiveView()) {
      const moveLabel =
        mainTab === "savings"
          ? "가입중인 저축으로 이동"
          : mainTab === "insurance"
            ? "납입중인 보험으로 이동"
            : mainTab === "loan"
              ? "상환중인 대출로 이동"
              : "사용중인 카드로 이동";
      return (
        <div className="cb-prod__toolbar">
          <button type="button" className="cb-catmgmt__miniBtn" onClick={deleteSelected}>
            삭제
          </button>
          <button type="button" className="cb-catmgmt__miniBtn" onClick={() => moveStatus("active")}>
            {moveLabel}
          </button>
        </div>
      );
    }

    function moveActiveStatus() {
      if (mainTab === "loan") moveStatus("matured");
      else if (mainTab === "card") moveStatus("cancelled");
      else moveStatus("matured");
    }

    function moveTerminateStatus() {
      if (mainTab === "loan") moveStatus("prepaid");
      else moveStatus("terminated");
    }

    return (
      <div className="cb-prod__toolbar cb-prod__toolbar--split">
        <div className="cb-prod__toolbarLeft">
          <div className="cb-catmgmt__orderActions">
            <button type="button" className="cb-catmgmt__orderBtn" onClick={() => reorderSelected(-1)}>
              ▲
            </button>
            <button type="button" className="cb-catmgmt__orderBtn" onClick={() => reorderSelected(1)}>
              ▼
            </button>
          </div>
          {mainTab === "card" ? (
            <>
              <button
                type="button"
                className="cb-catmgmt__miniBtn"
                disabled={syncingCards}
                onClick={() => void handleSyncCardsFromTransactions()}
              >
                {syncingCards ? "가져오는 중…" : "내역에서 가져오기"}
              </button>
              <button type="button" className="cb-catmgmt__miniBtn" onClick={() => moveStatus("cancelled")}>
                카드해지
              </button>
            </>
          ) : (
            <>
              <button type="button" className="cb-catmgmt__miniBtn" onClick={moveActiveStatus}>
                만기
              </button>
              <button type="button" className="cb-catmgmt__miniBtn" onClick={moveTerminateStatus}>
                {mainTab === "loan" ? "중도상환" : "중도해지"}
              </button>
            </>
          )}
          <button type="button" className="cb-catmgmt__miniBtn" onClick={deleteSelected}>
            삭제
          </button>
        </div>
        <button type="button" className="cb-prod__addBtn" onClick={addProduct}>
          상품추가 +
        </button>
      </div>
    );
  }

  return (
    <MonetaPanel
      wide
      tabs={[
        { id: "savings", label: "저축" },
        { id: "insurance", label: "보험" },
        { id: "loan", label: "대출" },
        { id: "card", label: "카드" },
      ]}
      activeTab={mainTab}
      onTabChange={(id) => {
        setMainTab(id as MainTab);
        setChecked(new Set());
        setSelectedIdx(0);
        setErr(null);
      }}
      subNav={renderSubNav()}
      hint={renderHint()}
      err={err}
      onSave={() => void handleSave()}
      onClose={handleClose}
      closeLabel="선택 초기화"
    >
      {loading ? <p className="cb-muted">불러오는 중…</p> : null}
      {!loading ? (
        <div className="cb-catmgmt__stack">
          <div className="cb-panel cb-panel--excel cb-prod__panel">
            <div className="cb-panel__tablewrap">
              <div className="cb-panel__tablescroll">{renderTable()}</div>
            </div>
          </div>
          {renderToolbar()}
        </div>
      ) : null}
    </MonetaPanel>
  );
}
