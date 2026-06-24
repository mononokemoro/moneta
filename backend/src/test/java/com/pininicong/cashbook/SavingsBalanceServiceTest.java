package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.repo.CbFinancialProductRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CashbookService;
import com.pininicong.cashbook.service.SavingsBalanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SavingsBalanceServiceTest {

    private static final String PRODUCT_NAME = "SavingsBalanceSvcTest 적금";

    @Autowired SavingsBalanceService balanceService;
    @Autowired CashbookService cashbookService;
    @Autowired CbFinancialProductRepository productRepo;
    @Autowired CbTransactionRepository txRepo;

    private Long productId;

    @BeforeEach
    void seedProduct() {
        CbFinancialProduct p = new CbFinancialProduct();
        p.setBook(LedgerBook.PERSONAL);
        p.setProductType(ProductType.SAVINGS);
        p.setName(PRODUCT_NAME);
        p.setOpeningBalance(new BigDecimal("100000"));
        p.setOpeningDate(LocalDate.of(2026, 1, 1));
        productId = productRepo.save(p).getId();
    }

    @Test
    void balanceUsesOpeningAndFlows() {
        saveSavings(LocalDate.of(2026, 1, 10), "50000");
        saveSavings(LocalDate.of(2026, 1, 20), "-10000");

        BigDecimal bal = balanceService.balanceAsOf(LedgerBook.PERSONAL, productId, PRODUCT_NAME, LocalDate.of(2026, 1, 20));
        assertThat(bal).isEqualByComparingTo("140000");
    }

    @Test
    void anchorResetsBalanceBase() {
        saveSavings(LocalDate.of(2026, 2, 1), "20000");
        balanceService.upsertAnchor(
                LedgerBook.PERSONAL,
                productId,
                null,
                LocalDate.of(2026, 2, 1),
                new BigDecimal("200000"),
                "명세서 대조");
        saveSavings(LocalDate.of(2026, 2, 15), "5000");

        BigDecimal bal = balanceService.balanceAsOf(LedgerBook.PERSONAL, productId, PRODUCT_NAME, LocalDate.of(2026, 2, 15));
        assertThat(bal).isEqualByComparingTo("205000");
    }

    @Test
    void dayViewShowsComputedAccumulated() {
        saveSavings(LocalDate.of(2026, 3, 5), "30000");
        var day = cashbookService.getDay(LocalDate.of(2026, 3, 5), LedgerBook.PERSONAL);
        var row =
                day.savings().stream()
                        .filter(s -> PRODUCT_NAME.equals(s.title()))
                        .findFirst()
                        .orElseThrow();
        assertThat(row.accumulatedAmount()).isEqualByComparingTo("130000");
    }

    private void saveSavings(LocalDate date, String amount) {
        cashbookService.createTransaction(
                new TransactionCreateRequest(
                        date,
                        TxType.SAVINGS,
                        PRODUCT_NAME,
                        new BigDecimal(amount),
                        null,
                        "저축",
                        null,
                        "",
                        productId,
                        "",
                        null,
                        LedgerBook.PERSONAL,
                        null,
                        null,
                        null));
    }
}
