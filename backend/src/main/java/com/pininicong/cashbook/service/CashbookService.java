package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CbCashBalance;
import com.pininicong.cashbook.domain.CbDailySheet;
import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.CbMonthlyBudget;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.DailySheetKey;
import com.pininicong.cashbook.domain.ExpenseScope;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.MonthlyBudgetKey;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.BudgetDto;
import com.pininicong.cashbook.dto.BudgetUpdateRequest;
import com.pininicong.cashbook.dto.CalendarMarkersDto;
import com.pininicong.cashbook.dto.CashBalanceUpdateRequest;
import com.pininicong.cashbook.dto.CreatedTransactionDto;
import com.pininicong.cashbook.dto.DailySheetUpdateRequest;
import com.pininicong.cashbook.dto.DayTransactionTableResponse;
import com.pininicong.cashbook.dto.DayViewDto;
import com.pininicong.cashbook.dto.FixedItemDto;
import com.pininicong.cashbook.dto.FixedItemSendRequest;
import com.pininicong.cashbook.dto.PaymentSummaryDto;
import com.pininicong.cashbook.dto.SavingsRowDto;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionMoveRequest;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;
import com.pininicong.cashbook.dto.TransactionTableRowDto;
import com.pininicong.cashbook.support.TransactionTableSupport;
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
    private final TransactionCategorySupport categorySupport;
    private final TransactionCardSupport cardSupport;
    private final TransactionSavingsProductSupport savingsProductSupport;
    private final SavingsBalanceService savingsBalanceService;

    public CashbookService(
            CbTransactionRepository txRepo,
            CbMonthlyBudgetRepository budgetRepo,
            CbDailySheetRepository sheetRepo,
            CbCashBalanceRepository cashRepo,
            CbFixedItemRepository fixedRepo,
            TransactionCategorySupport categorySupport,
            TransactionCardSupport cardSupport,
            TransactionSavingsProductSupport savingsProductSupport,
            SavingsBalanceService savingsBalanceService) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.sheetRepo = sheetRepo;
        this.cashRepo = cashRepo;
        this.fixedRepo = fixedRepo;
        this.categorySupport = categorySupport;
        this.cardSupport = cardSupport;
        this.savingsProductSupport = savingsProductSupport;
        this.savingsBalanceService = savingsBalanceService;
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
        Map<Long, BigDecimal> savingsBalances =
                savingsBalanceService.balancesForDayRows(ledger, sav, date);

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

        List<CbFixedItem> allFixed = fixedRepo.findByBookOrderBySortOrderAscIdAsc(ledger);
        List<CbFixedItem> fixedItems =
                allFixed.stream()
                        .filter(f -> com.pininicong.cashbook.support.FixedScheduleUtil.matchesDate(f, date))
                        .collect(Collectors.toList());

        Map<Long, String> expenseNames =
                categorySupport.minorNameIndex(ledger, CategoryType.EXPENSE);
        Map<Long, String> incomeNames = categorySupport.minorNameIndex(ledger, CategoryType.INCOME);
        Map<Long, String> householdNames =
                categorySupport.minorNameIndex(LedgerBook.HOUSEHOLD, CategoryType.EXPENSE);
        Map<Long, String> cardNames = cardSupport.cardNameIndex(ledger);

        return new DayViewDto(
                ledger.name(),
                ledger.label(),
                date.toString(),
                ymStr,
                budget,
                cashBal,
                exp.stream()
                        .map(t -> toExpenseIncomeRow(t, expenseNames, householdNames, cardNames))
                        .collect(Collectors.toList()),
                inc.stream()
                        .map(t -> toExpenseIncomeRow(t, incomeNames, householdNames, cardNames))
                        .collect(Collectors.toList()),
                sav.stream()
                        .map(t -> toSavingsRow(t, savingsBalances.getOrDefault(t.getId(), ZERO)))
                        .collect(Collectors.toList()),
                sheet.getScheduleNote() != null ? sheet.getScheduleNote() : "",
                sheet.getDayMemo() != null ? sheet.getDayMemo() : "",
                summarizePayments(exp, expenseNames, cardNames),
                summarizePayments(monthExp, expenseNames, cardNames),
                fixedItems.stream().map(FixedItemService::toRow).collect(Collectors.toList()));
    }

    public DayTransactionTableResponse getDayTransactionTable(LocalDate date, LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        List<TransactionTableRowDto> rows = new ArrayList<>();
        for (TxType type : TxType.values()) {
            txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(ledger, date, type)
                    .forEach(tx -> rows.add(TransactionTableSupport.toRow(tx)));
        }
        return new DayTransactionTableResponse(
                TransactionTableSupport.TABLE_NAME,
                date.toString(),
                ledger.name(),
                ledger.label(),
                rows.size(),
                TransactionTableSupport.queryForDay(ledger, date),
                rows);
    }

    public CalendarMarkersDto getCalendarMarkers(LedgerBook book, YearMonth ym) {
        LedgerBook ledger = bookOrDefault(book);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        List<LocalDate> dates =
                txRepo.findDistinctEntryDates(
                        ledger,
                        monthStart,
                        monthEnd,
                        List.of(TxType.EXPENSE, TxType.INCOME, TxType.SAVINGS));
        return new CalendarMarkersDto(dates);
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

    private PaymentSummaryDto summarizePayments(
            List<CbTransaction> expenseRows,
            Map<Long, String> categoryNames,
            Map<Long, String> cardNames) {
        BigDecimal cash = ZERO;
        BigDecimal credit = ZERO;
        BigDecimal debit = ZERO;
        BigDecimal other = ZERO;
        for (CbTransaction t : expenseRows) {
            BigDecimal a = n(t.getAmount());
            if (t.getCardProductId() == null) {
                cash = cash.add(a);
                continue;
            }
            String cn = cardNames.getOrDefault(t.getCardProductId(), "");
            String cat =
                    t.getCategoryId() != null
                            ? categoryNames.getOrDefault(t.getCategoryId(), "")
                            : "";
            if (cat.contains("체크") || cn.contains("체크")) {
                debit = debit.add(a);
            } else if (cat.contains("신용") || cn.contains("신용")) {
                credit = credit.add(a);
            } else {
                other = other.add(a);
            }
        }
        return new PaymentSummaryDto(cash, credit, debit, other);
    }

    private static TransactionRowDto toExpenseIncomeRow(
            CbTransaction t,
            Map<Long, String> categoryNames,
            Map<Long, String> householdNames,
            Map<Long, String> cardNames) {
        ExpenseScope scope = t.getExpenseScope() != null ? t.getExpenseScope() : ExpenseScope.NORMAL;
        Long categoryId = t.getCategoryId();
        Long householdCategoryId = t.getHouseholdCategoryId();
        Long cardProductId = t.getCardProductId();
        return new TransactionRowDto(
                t.getId(),
                t.getTitle(),
                n(t.getAmount()),
                categoryId,
                categoryId != null ? categoryNames.getOrDefault(categoryId, "") : "",
                cardProductId,
                cardProductId != null ? cardNames.getOrDefault(cardProductId, "") : "",
                t.getRemarks() != null ? t.getRemarks() : "",
                scope.name(),
                householdCategoryId,
                householdCategoryId != null
                        ? householdNames.getOrDefault(householdCategoryId, "")
                        : "");
    }

    private static SavingsRowDto toSavingsRow(CbTransaction t, BigDecimal computedBalance) {
        return new SavingsRowDto(
                t.getId(),
                t.getTitle(),
                n(t.getAmount()),
                computedBalance != null ? computedBalance : ZERO,
                t.getRemarks() != null ? t.getRemarks() : "",
                t.getSavingsProductId());
    }

    private static BigDecimal n(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    @Transactional
    public CreatedTransactionDto createTransaction(TransactionCreateRequest req) {
        LedgerBook book = bookOrDefault(req.book());
        ExpenseScope scope = expenseScopeOrDefault(req.expenseScope());
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
        applyPrimaryCategory(t, req.categoryId(), req.category());
        applyCardProduct(t, req.cardProductId(), req.cardName());
        applySavingsProduct(t, req.savingsProductId(), req.title());
        t.setRemarks(req.remarks() != null ? req.remarks() : "");
        if (req.txType() == TxType.EXPENSE) {
            t.setExpenseScope(scope);
            applyHouseholdCategory(t, req.householdCategoryId(), req.householdCategory());
        }
        if (req.txType() == TxType.SAVINGS) {
            t.setAccumulatedAmount(ZERO);
        }
        t.setSortOrder(nextOrder);
        txRepo.save(t);

        if (req.txType() == TxType.EXPENSE) {
            if (book == LedgerBook.PERSONAL && hasHouseholdMirror(t)) {
                linkHouseholdMirror(t);
            } else if (scope == ExpenseScope.COMMON) {
                linkMirror(t);
            }
        }
        return new CreatedTransactionDto(t.getId(), t.getTxType());
    }

    @Transactional
    public void deleteTransaction(Long id) {
        txRepo.findById(id).ifPresent(this::deleteWithLinked);
    }

    @Transactional
    public void moveTransactions(TransactionMoveRequest req) {
        LedgerBook book = bookOrDefault(req.book());
        List<CbTransaction> txs = new ArrayList<>();
        for (Long id : req.ids()) {
            CbTransaction t =
                    txRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("거래 없음: " + id));
            if (t.getBook() != book) {
                throw new IllegalArgumentException("거래 장부 불일치: " + id);
            }
            txs.add(t);
        }
        txs.sort(Comparator.comparing(CbTransaction::getId));

        Set<Long> processed = new HashSet<>();
        for (CbTransaction t : txs) {
            moveTransactionWithLinked(t, req.targetDate(), processed);
        }
    }

    private void moveTransactionWithLinked(
            CbTransaction t, LocalDate targetDate, Set<Long> processed) {
        if (!processed.add(t.getId())) return;
        moveTransactionToDate(t, targetDate);
        if (t.getLinkedTxId() != null) {
            txRepo.findById(t.getLinkedTxId())
                    .ifPresent(
                            linked -> {
                                if (processed.add(linked.getId())) {
                                    moveTransactionToDate(linked, targetDate);
                                }
                            });
        }
    }

    private void moveTransactionToDate(CbTransaction t, LocalDate targetDate) {
        if (t.getTxDate().equals(targetDate)) return;
        List<CbTransaction> same =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        t.getBook(), targetDate, t.getTxType());
        t.setTxDate(targetDate);
        t.setSortOrder(same.size());
        txRepo.save(t);
    }

    @Transactional
    public void updateTransaction(Long id, TransactionUpdateRequest req) {
        CbTransaction t =
                txRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("거래 없음: " + id));
        ExpenseScope nextScope =
                t.getTxType() == TxType.EXPENSE
                        ? expenseScopeOrDefault(req.expenseScope())
                        : ExpenseScope.NORMAL;
        ExpenseScope prevScope = expenseScopeOrDefault(t.getExpenseScope());
        Long prevHouseholdCategoryId = t.getHouseholdCategoryId();

        if (req.txDate() != null && !req.txDate().equals(t.getTxDate())) {
            moveTransactionToDate(t, req.txDate());
        }
        applyTransactionFields(t, req);
        if (t.getTxType() == TxType.EXPENSE) {
            t.setExpenseScope(nextScope);
        }

        if (t.getTxType() == TxType.EXPENSE) {
            if (t.getBook() == LedgerBook.PERSONAL) {
                if (applyPersonalHouseholdMirror(t, prevHouseholdCategoryId)) {
                    return;
                }
                if (!hasHouseholdMirror(t) && applyCommonExpenseScopeMirror(t, prevScope, nextScope)) {
                    return;
                }
            } else if (applyCommonExpenseScopeMirror(t, prevScope, nextScope)) {
                return;
            }
        }

        txRepo.save(t);
    }

    private boolean applyPersonalHouseholdMirror(CbTransaction t, Long prevHouseholdCategoryId) {
        boolean prev = prevHouseholdCategoryId != null;
        boolean next = hasHouseholdMirror(t);
        if (prev && !next) {
            unlinkMirror(t);
            return false;
        }
        if (!prev && next) {
            txRepo.save(t);
            linkHouseholdMirror(t);
            return true;
        }
        if (next && t.getLinkedTxId() != null) {
            syncHouseholdMirror(t);
        }
        return false;
    }

    private boolean applyCommonExpenseScopeMirror(
            CbTransaction t, ExpenseScope prevScope, ExpenseScope nextScope) {
        if (prevScope == ExpenseScope.COMMON && nextScope != ExpenseScope.COMMON) {
            unlinkMirror(t);
            return false;
        }
        if (prevScope != ExpenseScope.COMMON && nextScope == ExpenseScope.COMMON) {
            txRepo.save(t);
            linkMirror(t);
            return true;
        }
        if (nextScope == ExpenseScope.COMMON && t.getLinkedTxId() != null) {
            syncMirror(t);
        } else if (t.getLinkedTxId() != null) {
            syncMirrorSource(t);
        }
        return false;
    }

    private static boolean hasHouseholdMirror(CbTransaction t) {
        return t.getBook() == LedgerBook.PERSONAL
                && t.getTxType() == TxType.EXPENSE
                && t.getHouseholdCategoryId() != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static LedgerBook mirrorBook(LedgerBook book) {
        return book == LedgerBook.HOUSEHOLD ? LedgerBook.PERSONAL : LedgerBook.HOUSEHOLD;
    }

    private void deleteWithLinked(CbTransaction t) {
        Long linkedId = t.getLinkedTxId();
        txRepo.delete(t);
        if (linkedId != null) {
            txRepo.findById(linkedId).ifPresent(linked -> {
                linked.setLinkedTxId(null);
                txRepo.delete(linked);
            });
        }
    }

    private void linkHouseholdMirror(CbTransaction source) {
        LedgerBook targetBook = mirrorBook(source.getBook());
        List<CbTransaction> same =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        targetBook, source.getTxDate(), TxType.EXPENSE);
        CbTransaction mirror = new CbTransaction();
        mirror.setBook(targetBook);
        mirror.setTxDate(source.getTxDate());
        mirror.setTxType(TxType.EXPENSE);
        copyExpenseFieldsExceptCategoryWithCard(source, mirror);
        mirror.setCategoryId(source.getHouseholdCategoryId());
        mirror.setExpenseScope(ExpenseScope.NORMAL);
        mirror.setSortOrder(same.size());
        txRepo.save(mirror);

        source.setLinkedTxId(mirror.getId());
        mirror.setLinkedTxId(source.getId());
        txRepo.save(mirror);
        txRepo.save(source);
    }

    private void syncHouseholdMirror(CbTransaction source) {
        if (source.getLinkedTxId() == null) return;
        LedgerBook targetBook = mirrorBook(source.getBook());
        txRepo.findById(source.getLinkedTxId())
                .filter(m -> m.getBook() == targetBook)
                .ifPresent(
                        mirror -> {
                            copyExpenseFieldsExceptCategoryWithCard(source, mirror);
                            mirror.setCategoryId(source.getHouseholdCategoryId());
                            txRepo.save(mirror);
                        });
    }

    private static void copyExpenseFieldsExceptCategory(CbTransaction from, CbTransaction to) {
        to.setTitle(from.getTitle());
        to.setAmount(from.getAmount());
        to.setRemarks(from.getRemarks() != null ? from.getRemarks() : "");
    }

    private void copyExpenseFieldsExceptCategoryWithCard(CbTransaction from, CbTransaction to) {
        copyExpenseFieldsExceptCategory(from, to);
        to.setCardProductId(
                cardSupport
                        .mirrorCardId(from.getBook(), to.getBook(), from.getCardProductId())
                        .orElse(null));
    }

    private void linkMirror(CbTransaction source) {
        LedgerBook targetBook = mirrorBook(source.getBook());
        List<CbTransaction> same =
                txRepo.findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
                        targetBook, source.getTxDate(), TxType.EXPENSE);
        CbTransaction mirror = new CbTransaction();
        mirror.setBook(targetBook);
        mirror.setTxDate(source.getTxDate());
        mirror.setTxType(TxType.EXPENSE);
        copyExpenseFields(source, mirror);
        mirror.setExpenseScope(ExpenseScope.NORMAL);
        mirror.setSortOrder(same.size());
        txRepo.save(mirror);

        source.setLinkedTxId(mirror.getId());
        mirror.setLinkedTxId(source.getId());
        txRepo.save(mirror);
        txRepo.save(source);
    }

    private void unlinkMirror(CbTransaction source) {
        Long linkedId = source.getLinkedTxId();
        source.setLinkedTxId(null);
        if (linkedId != null) {
            txRepo.findById(linkedId).ifPresent(linked -> {
                linked.setLinkedTxId(null);
                txRepo.delete(linked);
            });
        }
    }

    private void syncMirror(CbTransaction source) {
        if (source.getLinkedTxId() == null) return;
        LedgerBook targetBook = mirrorBook(source.getBook());
        txRepo.findById(source.getLinkedTxId())
                .filter(m -> m.getBook() == targetBook)
                .ifPresent(
                        mirror -> {
                            copyExpenseFields(source, mirror);
                            txRepo.save(mirror);
                        });
    }

    private void syncMirrorSource(CbTransaction mirror) {
        if (mirror.getLinkedTxId() == null) return;
        LedgerBook sourceBook = mirrorBook(mirror.getBook());
        txRepo.findById(mirror.getLinkedTxId())
                .filter(s -> s.getBook() == sourceBook)
                .ifPresent(
                        source -> {
                            copyExpenseFields(mirror, source);
                            txRepo.save(source);
                        });
    }

    private void copyExpenseFields(CbTransaction from, CbTransaction to) {
        to.setTitle(from.getTitle());
        to.setAmount(from.getAmount());
        to.setCategoryId(from.getCategoryId());
        to.setCardProductId(
                cardSupport
                        .mirrorCardId(from.getBook(), to.getBook(), from.getCardProductId())
                        .orElse(null));
        to.setRemarks(from.getRemarks() != null ? from.getRemarks() : "");
    }

    private void applyTransactionFields(CbTransaction t, TransactionUpdateRequest req) {
        t.setTitle(req.title() != null ? req.title() : "");
        t.setAmount(req.amount());
        applyPrimaryCategory(t, req.categoryId(), req.category());
        applyCardProduct(t, req.cardProductId(), req.cardName());
        applySavingsProduct(t, req.savingsProductId(), req.title());
        t.setRemarks(req.remarks() != null ? req.remarks() : "");
        if (t.getTxType() == TxType.EXPENSE) {
            applyHouseholdCategory(t, req.householdCategoryId(), req.householdCategory());
        }
        if (t.getTxType() == TxType.SAVINGS) {
            t.setAccumulatedAmount(ZERO);
        }
    }

    private void applyPrimaryCategory(CbTransaction t, Long categoryId, String categoryName) {
        CategoryType type = TransactionCategorySupport.categoryTypeFor(t.getTxType());
        var resolved = categorySupport.resolveMinor(t.getBook(), type, categoryId, categoryName);
        t.setCategoryId(resolved.map(TransactionCategorySupport.ResolvedCategory::id).orElse(null));
    }

    private void applyHouseholdCategory(CbTransaction t, Long categoryId, String categoryName) {
        if (t.getBook() != LedgerBook.PERSONAL || t.getTxType() != TxType.EXPENSE) {
            return;
        }
        if (categoryId == null && (categoryName == null || categoryName.isBlank())) {
            t.setHouseholdCategoryId(null);
            return;
        }
        var resolved =
                categorySupport.resolveMinor(
                        LedgerBook.HOUSEHOLD, CategoryType.EXPENSE, categoryId, categoryName);
        t.setHouseholdCategoryId(
                resolved.map(TransactionCategorySupport.ResolvedCategory::id).orElse(null));
    }

    private void applyCardProduct(CbTransaction t, Long cardProductId, String cardName) {
        if (t.getTxType() != TxType.EXPENSE) {
            t.setCardProductId(null);
            return;
        }
        if (cardProductId == null && (cardName == null || cardName.isBlank())) {
            t.setCardProductId(null);
            return;
        }
        var resolved = cardSupport.resolveCard(t.getBook(), cardProductId, cardName);
        t.setCardProductId(resolved.map(TransactionCardSupport.ResolvedCard::id).orElse(null));
    }

    private void applySavingsProduct(CbTransaction t, Long savingsProductId, String title) {
        if (t.getTxType() != TxType.SAVINGS) {
            t.setSavingsProductId(null);
            return;
        }
        savingsProductSupport.applySavingsProduct(t, savingsProductId, title);
    }

    private static ExpenseScope expenseScopeOrDefault(ExpenseScope scope) {
        return scope != null ? scope : ExpenseScope.NORMAL;
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
            applyPrimaryCategory(
                    t,
                    null,
                    fixed.getCategory() != null ? fixed.getCategory() : "");
            applyCardProduct(t, null, fixed.getCardName());
            applySavingsProduct(t, null, fixed.getTitle());
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
