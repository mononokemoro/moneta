package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbCashBalance;
import com.pininicong.cashbook.domain.CbDailySheet;
import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.CbMonthlyBudget;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.DailySheetKey;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.MonthlyBudgetKey;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.BudgetDto;
import com.pininicong.cashbook.dto.BudgetUpdateRequest;
import com.pininicong.cashbook.dto.CashBalanceUpdateRequest;
import com.pininicong.cashbook.dto.CreatedTransactionDto;
import com.pininicong.cashbook.dto.DailySheetUpdateRequest;
import com.pininicong.cashbook.dto.DayViewDto;
import com.pininicong.cashbook.dto.FixedItemDto;
import com.pininicong.cashbook.dto.FixedItemSendRequest;
import com.pininicong.cashbook.dto.PaymentSummaryDto;
import com.pininicong.cashbook.dto.SavingsRowDto;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionRowDto;
import com.pininicong.cashbook.dto.TransactionUpdateRequest;
import com.pininicong.cashbook.repo.CbCashBalanceRepository;
import com.pininicong.cashbook.repo.CbDailySheetRepository;
import com.pininicong.cashbook.repo.CbFixedItemRepository;
import com.pininicong.cashbook.repo.CbMonthlyBudgetRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CashbookService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CbTransactionRepository txRepo;
    private final CbMonthlyBudgetRepository budgetRepo;
    private final CbDailySheetRepository sheetRepo;
    private final CbCashBalanceRepository cashRepo;
    private final CbFixedItemRepository fixedRepo;

    public CashbookService(
            CbTransactionRepository txRepo,
            CbMonthlyBudgetRepository budgetRepo,
            CbDailySheetRepository sheetRepo,
            CbCashBalanceRepository cashRepo,
            CbFixedItemRepository fixedRepo) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.sheetRepo = sheetRepo;
        this.cashRepo = cashRepo;
        this.fixedRepo = fixedRepo;
    }

    public DayViewDto getDay(LocalDate date, LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        YearMonth ym = YearMonth.from(date);
        String ymStr = ym.toString();
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        BigDecimal spentMonth = txRepo.sumAmountBetween(ledger, TxType.EXPENSE, monthStart, monthEnd);
        if (spentMonth == null) {
            spentMonth = ZERO;
        }

        CbMonthlyBudget mb =
                budgetRepo
                        .findById(new MonthlyBudgetKey(ledger, ymStr))
                        .orElseGet(
                                () -> {
                                    CbMonthlyBudget b = new CbMonthlyBudget();
                                    b.setBook(ledger);
                                    b.setYearMonth(ymStr);
                                    b.setTotalBudget(ZERO);
                                    return b;
                                });
        BigDecimal totalBudget = n(mb.getTotalBudget());
        BigDecimal remaining = totalBudget.subtract(spentMonth);

        BudgetDto budget =
                new BudgetDto(totalBudget, spentMonth, remaining, formatBudgetPeriod(ym));

        BigDecimal cashBal =
                cashRepo.findById(ledger).map(CbCashBalance::getAmount).orElse(ZERO);

        List<CbTransaction> exp =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        ledger, date, TxType.EXPENSE);
        List<CbTransaction> monthExp =
                txRepo.findByBookAndTxDateBetween(ledger, monthStart, monthEnd).stream()
                        .filter(t -> t.getTxType() == TxType.EXPENSE)
                        .collect(Collectors.toList());
        List<CbTransaction> inc =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        ledger, date, TxType.INCOME);
        List<CbTransaction> sav =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        ledger, date, TxType.SAVINGS);

        CbDailySheet sheet =
                sheetRepo
                        .findById(new DailySheetKey(ledger, date))
                        .orElseGet(
                                () -> {
                                    CbDailySheet s = new CbDailySheet();
                                    s.setBook(ledger);
                                    s.setSheetDate(date);
                                    return s;
                                });

        List<CbFixedItem> fixedItems = fixedRepo.findByBookOrderBySortOrderAscIdAsc(ledger);

        return new DayViewDto(
                ledger.name(),
                ledger.label(),
                date.toString(),
                ymStr,
                budget,
                cashBal,
                exp.stream().map(CashbookService::toExpenseIncomeRow).collect(Collectors.toList()),
                inc.stream().map(CashbookService::toExpenseIncomeRow).collect(Collectors.toList()),
                sav.stream().map(CashbookService::toSavingsRow).collect(Collectors.toList()),
                sheet.getScheduleNote() != null ? sheet.getScheduleNote() : "",
                sheet.getDayMemo() != null ? sheet.getDayMemo() : "",
                summarizePayments(exp),
                summarizePayments(monthExp),
                fixedItems.stream().map(CashbookService::toFixedItem).collect(Collectors.toList()));
    }

    private static LedgerBook bookOrDefault(LedgerBook book) {
        return book != null ? book : LedgerBook.PERSONAL;
    }

    private static String formatBudgetPeriod(YearMonth ym) {
        LocalDate s = ym.atDay(1);
        LocalDate e = ym.atEndOfMonth();
        return String.format(
                "%02d.%02d~%02d.%02d",
                s.getMonthValue(), s.getDayOfMonth(), e.getMonthValue(), e.getDayOfMonth());
    }

    private static PaymentSummaryDto summarizePayments(List<CbTransaction> expenseRows) {
        BigDecimal cash = ZERO;
        BigDecimal credit = ZERO;
        BigDecimal debit = ZERO;
        BigDecimal other = ZERO;
        for (CbTransaction t : expenseRows) {
            BigDecimal a = n(t.getAmount());
            String cn = t.getCardName();
            if (cn == null || cn.isBlank()) {
                cash = cash.add(a);
                continue;
            }
            String cat = t.getCategory() != null ? t.getCategory() : "";
            if (cat.contains("체크")) {
                debit = debit.add(a);
            } else if (cat.contains("신용")) {
                credit = credit.add(a);
            } else {
                other = other.add(a);
            }
        }
        return new PaymentSummaryDto(cash, credit, debit, other);
    }

    private static TransactionRowDto toExpenseIncomeRow(CbTransaction t) {
        return new TransactionRowDto(
                t.getId(),
                t.getTitle(),
                n(t.getAmount()),
                t.getCategory() != null ? t.getCategory() : "",
                t.getCardName() != null ? t.getCardName() : "",
                t.getRemarks() != null ? t.getRemarks() : "");
    }

    private static SavingsRowDto toSavingsRow(CbTransaction t) {
        return new SavingsRowDto(
                t.getId(),
                t.getTitle(),
                n(t.getAmount()),
                t.getAccumulatedAmount() != null ? t.getAccumulatedAmount() : ZERO,
                t.getRemarks() != null ? t.getRemarks() : "");
    }

    private static FixedItemDto toFixedItem(CbFixedItem f) {
        return new FixedItemDto(
                f.getId(),
                f.getTitle(),
                n(f.getDefaultAmount()),
                f.getCategory() != null ? f.getCategory() : "",
                f.getCardName() != null ? f.getCardName() : "",
                f.getTxType());
    }

    private static BigDecimal n(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    @Transactional
    public CreatedTransactionDto createTransaction(TransactionCreateRequest req) {
        LedgerBook book = bookOrDefault(req.book());
        List<CbTransaction> same =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        book, req.txDate(), req.txType());
        int nextOrder = same.size();

        CbTransaction t = new CbTransaction();
        t.setBook(book);
        t.setTxDate(req.txDate());
        t.setTxType(req.txType());
        t.setTitle(req.title() != null ? req.title() : "");
        t.setAmount(req.amount());
        t.setCategory(req.category() != null ? req.category() : "");
        t.setCardName(req.cardName() != null ? req.cardName() : "");
        t.setRemarks(req.remarks() != null ? req.remarks() : "");
        if (req.txType() == TxType.SAVINGS) {
            t.setAccumulatedAmount(req.accumulatedAmount() != null ? req.accumulatedAmount() : ZERO);
        }
        t.setSortOrder(nextOrder);
        txRepo.save(t);
        return new CreatedTransactionDto(t.getId(), t.getTxType());
    }

    @Transactional
    public void deleteTransaction(Long id) {
        txRepo.deleteById(id);
    }

    @Transactional
    public void updateTransaction(Long id, TransactionUpdateRequest req) {
        CbTransaction t =
                txRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("거래 없음: " + id));
        t.setTitle(req.title() != null ? req.title() : "");
        t.setAmount(req.amount());
        t.setCategory(req.category() != null ? req.category() : "");
        t.setCardName(req.cardName() != null ? req.cardName() : "");
        t.setRemarks(req.remarks() != null ? req.remarks() : "");
        if (t.getTxType() == TxType.SAVINGS) {
            t.setAccumulatedAmount(req.accumulatedAmount() != null ? req.accumulatedAmount() : ZERO);
        }
        txRepo.save(t);
    }

    @Transactional
    public int sendFixedItemsToCashbook(FixedItemSendRequest req) {
        LedgerBook book = bookOrDefault(req.book());
        int created = 0;
        for (FixedItemSendRequest.FixedItemSendEntry entry : req.entries()) {
            if (entry.amount() == null || entry.amount().signum() == 0) {
                continue;
            }
            CbFixedItem fixed =
                    fixedRepo
                            .findById(entry.fixedItemId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "고정 항목 없음: " + entry.fixedItemId()));
            if (fixed.getBook() != book) {
                throw new IllegalArgumentException("고정 항목 장부 불일치: " + entry.fixedItemId());
            }
            CbTransaction t = new CbTransaction();
            t.setBook(book);
            t.setTxDate(req.txDate());
            t.setTxType(fixed.getTxType());
            t.setTitle(fixed.getTitle());
            t.setAmount(entry.amount());
            t.setCategory(fixed.getCategory() != null ? fixed.getCategory() : "");
            t.setCardName(fixed.getCardName() != null ? fixed.getCardName() : "");
            t.setRemarks("고정항목");
            if (fixed.getTxType() == TxType.SAVINGS) {
                t.setAccumulatedAmount(ZERO);
            }
            List<CbTransaction> same =
                    txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                            book, req.txDate(), fixed.getTxType());
            t.setSortOrder(same.size());
            txRepo.save(t);
            created++;
        }
        return created;
    }

    @Transactional
    public void upsertBudget(LedgerBook book, String yearMonth, BudgetUpdateRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        CbMonthlyBudget b =
                budgetRepo
                        .findById(new MonthlyBudgetKey(ledger, yearMonth))
                        .orElseGet(
                                () -> {
                                    CbMonthlyBudget x = new CbMonthlyBudget();
                                    x.setBook(ledger);
                                    x.setYearMonth(yearMonth);
                                    return x;
                                });
        b.setTotalBudget(req.totalBudget());
        budgetRepo.save(b);
    }

    @Transactional
    public void updateCashBalance(LedgerBook book, CashBalanceUpdateRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        CbCashBalance c =
                cashRepo
                        .findById(ledger)
                        .orElseGet(
                                () -> {
                                    CbCashBalance x = new CbCashBalance();
                                    x.setBook(ledger);
                                    x.setAmount(ZERO);
                                    return x;
                                });
        c.setAmount(req.amount());
        cashRepo.save(c);
    }

    @Transactional
    public void upsertDailySheet(LedgerBook book, LocalDate date, DailySheetUpdateRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        CbDailySheet s =
                sheetRepo
                        .findById(new DailySheetKey(ledger, date))
                        .orElseGet(
                                () -> {
                                    CbDailySheet x = new CbDailySheet();
                                    x.setBook(ledger);
                                    x.setSheetDate(date);
                                    return x;
                                });
        if (req.scheduleNote() != null) {
            s.setScheduleNote(req.scheduleNote());
        }
        if (req.dayMemo() != null) {
            s.setDayMemo(req.dayMemo());
        }
        sheetRepo.save(s);
    }
}
