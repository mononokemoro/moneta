package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionMoveRequest;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CashbookService;
import com.pininicong.cashbook.service.CategoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TransactionMoveTest {

    @Autowired CashbookService cashbookService;
    @Autowired CbTransactionRepository txRepo;
    @Autowired CategoryService categoryService;

    @Test
    void movesSelectedTransactionsToTargetDate() {
        categoryService.migrateIncomeHierarchy(LedgerBook.PERSONAL);
        categoryService.migrateExpenseHierarchy(LedgerBook.PERSONAL);
        LocalDate from = LocalDate.of(2026, 6, 15);
        LocalDate to = LocalDate.of(2026, 6, 20);

        var first =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                from,
                                TxType.INCOME,
                                "네이버정산",
                                new BigDecimal("44125"),
                                null,
                                "수입제외",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.PERSONAL,
                                null,
                                null,
                                null));
        var second =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                from,
                                TxType.EXPENSE,
                                "점심",
                                new BigDecimal("9000"),
                                null,
                                "외식",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.PERSONAL,
                                null,
                                null,
                                null));

        cashbookService.moveTransactions(
                new TransactionMoveRequest(to, List.of(first.id(), second.id()), LedgerBook.PERSONAL));

        var movedFirst = txRepo.findById(first.id()).orElseThrow();
        var movedSecond = txRepo.findById(second.id()).orElseThrow();
        assertThat(movedFirst.getTxDate()).isEqualTo(to);
        assertThat(movedSecond.getTxDate()).isEqualTo(to);
        assertThat(movedFirst.getSortOrder()).isZero();
        assertThat(movedSecond.getSortOrder()).isZero();
    }
}
