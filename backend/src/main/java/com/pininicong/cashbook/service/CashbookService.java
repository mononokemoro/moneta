package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbCashBalance;
import com.pininicong.cashbook.domain.CbDailySheet;
import com.pininicong.cashbook.domain.CbMonthlyBudget;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.BudgetDto;
import com.pininicong.cashbook.dto.BudgetUpdateRequest;
import com.pininicong.cashbook.dto.CashBalanceUpdateRequest;
import com.pininicong.cashbook.dto.CreatedTransactionDto;
import com.pininicong.cashbook.dto.DailySheetUpdateRequest;
import com.pininicong.cashbook.dto.DayViewDto;
import com.pininicong.cashbook.dto.PaymentSummaryDto;
import com.pininicong.cashbook.dto.SavingsRowDto;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionRowDto;
import com.pininicong.cashbook.repo.CbCashBalanceRepository;
import com.pininicong.cashbook.repo.CbDailySheetRepository;
import com.pininicong.cashbook.repo.CbMonthlyBudgetRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

    public CashbookService(
            CbTransactionRepository txRepo,
            CbMonthlyBudgetRepository budgetRepo,
            CbDailySheetRepository sheetRepo,
            CbCashBalanceRepository cashRepo) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.sheetRepo = sheetRepo;
        this.cashRepo = cashRepo;
    }

    public DayViewDto getDay(LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        String ymStr = ym.toString();
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        BigDecimal spentMonth =
                txRepo.sumAmountBetween(TxType.EXPENSE, monthStart, monthEnd);
        if (spentMonth == null) {
            spentMonth = ZERO;
        }

        CbMonthlyBudget mb =
                budgetRepo
                        .findById(ymStr)
                        .orElseGet(
                                () -> {
                                    CbMonthlyBudget b = new CbMonthlyBudget();
                                    b.setYearMonth(ymStr);
                                    b.setTotalBudget(ZERO);
                                    return b;
                                });
        BigDecimal totalBudget = n(mb.getTotalBudget());
        BigDecimal remaining = totalBudget.subtract(spentMonth);

        BudgetDto budget =
                new BudgetDto(
                        totalBudget,
                        spentMonth,
                        remaining,
                        formatBudgetPeriod(ym));

        BigDecimal cashBal =
                cashRepo
                        .findById(CbCashBalance.SINGLETON_ID)
                        .map(CbCashBalance::getAmount)
                        .orElse(ZERO);

        List<CbTransaction> exp =
                txRepo.findByTxDateAndTxTypeOrderBySortOrderAscIdAsc(date, TxType.EXPENSE);
        List<CbTransaction> inc =
                txRepo.findByTxDateAndTxTypeOrderBySortOrderAscIdAsc(date, TxType.INCOME);
        List<CbTransaction> sav =
                txRepo.findByTxDateAndTxTypeOrderBySortOrderAscIdAsc(date, TxType.SAVINGS);

        CbDailySheet sheet =
                sheetRepo
                        .findById(date)
                        .orElseGet(
                                () -> {
                                    CbDailySheet s = new CbDailySheet();
                                    s.setSheetDate(date);
                                    return s;
                                });

        return new DayViewDto(
                date.toString(),
                ymStr,
                budget,
                cashBal,
                exp.stream().map(CashbookService::toExpenseIncomeRow).collect(Collectors.toList()),
                inc.stream().map(CashbookService::toExpenseIncomeRow).collect(Collectors.toList()),
                sav.stream().map(CashbookService::toSavingsRow).collect(Collectors.toList()),
                sheet.getScheduleNote() != null ? sheet.getScheduleNote() : "",
                sheet.getDayMemo() != null ? sheet.getDayMemo() : "",
                summarizePayments(exp));
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

    private static BigDecimal n(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    @Transactional
    public CreatedTransactionDto createTransaction(TransactionCreateRequest req) {
        List<CbTransaction> same =
                txRepo.findByTxDateAndTxTypeOrderBySortOrderAscIdAsc(req.txDate(), req.txType());
        int nextOrder = same.size();

        CbTransaction t = new CbTransaction();
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
    public void upsertBudget(String yearMonth, BudgetUpdateRequest req) {
        CbMonthlyBudget b =
                budgetRepo
                        .findById(yearMonth)
                        .orElseGet(
                                () -> {
                                    CbMonthlyBudget x = new CbMonthlyBudget();
                                    x.setYearMonth(yearMonth);
                                    return x;
                                });
        b.setTotalBudget(req.totalBudget());
        budgetRepo.save(b);
    }

    @Transactional
    public void updateCashBalance(CashBalanceUpdateRequest req) {
        CbCashBalance c =
                cashRepo
                        .findById(CbCashBalance.SINGLETON_ID)
                        .orElseGet(
                                () -> {
                                    CbCashBalance x = new CbCashBalance();
                                    x.setAmount(ZERO);
                                    return x;
                                });
        c.setAmount(req.amount());
        cashRepo.save(c);
    }

    @Transactional
    public void upsertDailySheet(LocalDate date, DailySheetUpdateRequest req) {
        CbDailySheet s =
                sheetRepo
                        .findById(date)
                        .orElseGet(
                                () -> {
                                    CbDailySheet x = new CbDailySheet();
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
