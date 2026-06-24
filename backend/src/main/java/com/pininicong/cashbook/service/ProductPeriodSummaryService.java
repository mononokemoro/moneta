package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.CbProductPeriodSummary;
import com.pininicong.cashbook.domain.CbProductPeriodSummary.PeriodType;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.repo.CbFinancialProductRepository;
import com.pininicong.cashbook.repo.CbProductPeriodSummaryRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 저축/보험 상품 월·연 집계 캐시 (보고서용). */
@Service
public class ProductPeriodSummaryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CbFinancialProductRepository productRepo;
    private final CbProductPeriodSummaryRepository summaryRepo;
    private final CbTransactionRepository txRepo;
    private final SavingsBalanceService balanceService;

    public ProductPeriodSummaryService(
            CbFinancialProductRepository productRepo,
            CbProductPeriodSummaryRepository summaryRepo,
            CbTransactionRepository txRepo,
            SavingsBalanceService balanceService) {
        this.productRepo = productRepo;
        this.summaryRepo = summaryRepo;
        this.txRepo = txRepo;
        this.balanceService = balanceService;
    }

    @Transactional
    public List<CbProductPeriodSummary> refreshMonth(LedgerBook book, YearMonth ym) {
        return refresh(book, PeriodType.MONTH, ym.toString(), ym.atDay(1), ym.atEndOfMonth());
    }

    @Transactional
    public List<CbProductPeriodSummary> refreshYear(LedgerBook book, Year year) {
        return refresh(
                book,
                PeriodType.YEAR,
                String.valueOf(year.getValue()),
                year.atDay(1),
                year.atMonth(12).atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public List<CbProductPeriodSummary> listMonth(LedgerBook book, YearMonth ym) {
        LedgerBook ledger = book != null ? book : LedgerBook.PERSONAL;
        return summaryRepo.findByBookAndPeriodTypeAndPeriodKey(
                ledger, PeriodType.MONTH, ym.toString());
    }

    private List<CbProductPeriodSummary> refresh(
            LedgerBook book, PeriodType periodType, String periodKey, LocalDate start, LocalDate end) {
        LedgerBook ledger = book != null ? book : LedgerBook.PERSONAL;
        summaryRepo.deleteByBookAndPeriodTypeAndPeriodKey(ledger, periodType, periodKey);

        List<CbProductPeriodSummary> saved = new ArrayList<>();
        List<CbFinancialProduct> products =
                productRepo.findByBookOrderByProductTypeAscSortOrderAscIdAsc(ledger);
        for (CbFinancialProduct product : products) {
            if (product.getProductType() != ProductType.SAVINGS
                    && product.getProductType() != ProductType.INSURANCE) {
                continue;
            }
            List<CbTransaction> flows =
                    txRepo.findSavingsByProductBetween(ledger, product.getId(), start, end);
            BigDecimal inflow = ZERO;
            BigDecimal outflow = ZERO;
            for (CbTransaction tx : flows) {
                BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : ZERO;
                if (amt.signum() >= 0) {
                    inflow = inflow.add(amt);
                } else {
                    outflow = outflow.add(amt.abs());
                }
            }
            BigDecimal net = inflow.subtract(outflow);
            BigDecimal endBalance =
                    balanceService.balanceAsOf(ledger, product.getId(), product.getName(), end);

            CbProductPeriodSummary row = new CbProductPeriodSummary();
            row.setBook(ledger);
            row.setProductId(product.getId());
            row.setPeriodType(periodType);
            row.setPeriodKey(periodKey);
            row.setInflow(inflow);
            row.setOutflow(outflow);
            row.setNetFlow(net);
            row.setEndBalance(endBalance);
            saved.add(summaryRepo.save(row));
        }
        return saved;
    }
}
