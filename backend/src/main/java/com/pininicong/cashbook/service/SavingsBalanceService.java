package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.CbProductBalanceAnchor;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.repo.CbBalanceAdjustmentRepository;
import com.pininicong.cashbook.repo.CbFinancialProductRepository;
import com.pininicong.cashbook.repo.CbProductBalanceAnchorRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SavingsBalanceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);

    private final CbFinancialProductRepository productRepo;
    private final CbProductBalanceAnchorRepository anchorRepo;
    private final CbBalanceAdjustmentRepository adjustmentRepo;
    private final CbTransactionRepository txRepo;
    private final TransactionSavingsProductSupport savingsProductSupport;

    public SavingsBalanceService(
            CbFinancialProductRepository productRepo,
            CbProductBalanceAnchorRepository anchorRepo,
            CbBalanceAdjustmentRepository adjustmentRepo,
            CbTransactionRepository txRepo,
            TransactionSavingsProductSupport savingsProductSupport) {
        this.productRepo = productRepo;
        this.anchorRepo = anchorRepo;
        this.adjustmentRepo = adjustmentRepo;
        this.txRepo = txRepo;
        this.savingsProductSupport = savingsProductSupport;
    }

    public BigDecimal balanceAsOf(LedgerBook book, Long productId, String titleFallback, LocalDate date) {
        LedgerBook ledger = book != null ? book : LedgerBook.PERSONAL;
        if (date == null) {
            return ZERO;
        }

        CbFinancialProduct product = resolveProduct(ledger, productId, titleFallback);
        if (product != null) {
            return balanceAsOfProduct(ledger, product, date);
        }
        if (titleFallback != null && !titleFallback.isBlank()) {
            return txRepo.sumSavingsTitleFlowUpTo(ledger, titleFallback.trim(), date);
        }
        return ZERO;
    }

    public Map<Long, BigDecimal> balancesForDayRows(
            LedgerBook book, List<CbTransaction> savingsRows, LocalDate date) {
        Map<Long, BigDecimal> cache = new HashMap<>();
        Map<String, BigDecimal> titleCache = new HashMap<>();
        Map<Long, BigDecimal> result = new HashMap<>();
        for (CbTransaction row : savingsRows) {
            Long productId = row.getSavingsProductId();
            if (productId != null) {
                result.put(
                        row.getId(),
                        cache.computeIfAbsent(
                                productId,
                                id ->
                                        balanceAsOf(
                                                book,
                                                id,
                                                row.getTitle(),
                                                date)));
                continue;
            }
            String title = row.getTitle() != null ? row.getTitle().trim() : "";
            CbFinancialProduct product =
                    savingsProductSupport.resolveProductEntity(book, null, title).orElse(null);
            if (product != null) {
                result.put(
                        row.getId(),
                        cache.computeIfAbsent(
                                product.getId(),
                                id -> balanceAsOfProduct(book, product, date)));
            } else {
                result.put(
                        row.getId(),
                        titleCache.computeIfAbsent(
                                title, t -> balanceAsOf(book, null, t, date)));
            }
        }
        return result;
    }

    @Transactional
    public CbProductBalanceAnchor upsertAnchor(
            LedgerBook book,
            Long productId,
            String titleFallback,
            LocalDate anchorDate,
            BigDecimal balance,
            String remarks) {
        LedgerBook ledger = book != null ? book : LedgerBook.PERSONAL;
        CbFinancialProduct product =
                resolveProduct(ledger, productId, titleFallback);
        if (product == null) {
            throw new IllegalArgumentException("저축/보험 상품을 찾을 수 없습니다.");
        }
        if (anchorDate == null) {
            throw new IllegalArgumentException("기준일이 필요합니다.");
        }
        BigDecimal bal = balance != null ? balance : ZERO;

        CbProductBalanceAnchor anchor =
                anchorRepo
                        .findByBookAndProductIdAndAnchorDate(ledger, product.getId(), anchorDate)
                        .orElseGet(CbProductBalanceAnchor::new);
        anchor.setBook(ledger);
        anchor.setProductId(product.getId());
        anchor.setAnchorDate(anchorDate);
        anchor.setBalance(bal);
        anchor.setRemarks(remarks != null ? remarks : "");
        return anchorRepo.save(anchor);
    }

    private BigDecimal balanceAsOfProduct(LedgerBook book, CbFinancialProduct product, LocalDate date) {
        LocalDate baseDate = MIN_DATE;
        BigDecimal baseBalance = ZERO;

        Optional<CbProductBalanceAnchor> anchor =
                anchorRepo.findTopByBookAndProductIdAndAnchorDateLessThanEqualOrderByAnchorDateDescIdDesc(
                        book, product.getId(), date);
        if (anchor.isPresent()) {
            baseDate = anchor.get().getAnchorDate();
            baseBalance = n(anchor.get().getBalance());
        } else {
            baseBalance = n(product.getOpeningBalance());
            if (product.getOpeningDate() != null) {
                baseDate = product.getOpeningDate();
            }
        }

        BigDecimal flow = txRepo.sumSavingsProductFlow(book, product.getId(), baseDate, date);
        BigDecimal adj = adjustmentRepo.sumBetween(book, product.getId(), baseDate, date);
        return baseBalance.add(flow).add(adj);
    }

    private CbFinancialProduct resolveProduct(LedgerBook book, Long productId, String titleFallback) {
        if (productId != null) {
            return productRepo.findByIdAndBook(productId, book).orElse(null);
        }
        return savingsProductSupport.resolveProductEntity(book, productId, titleFallback).orElse(null);
    }

    private static BigDecimal n(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
