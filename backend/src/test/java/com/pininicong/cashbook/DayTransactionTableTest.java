package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CashbookService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DayTransactionTableTest {

    @Autowired CashbookService cashbookService;

    @Autowired CbTransactionRepository txRepo;

    @Test
    void getDayTransactionTableReturnsCbTransactionColumns() {
        var date = LocalDate.of(2026, 6, 23);

        var expense = new com.pininicong.cashbook.domain.CbTransaction();
        expense.setBook(LedgerBook.PERSONAL);
        expense.setTxDate(date);
        expense.setTxType(TxType.EXPENSE);
        expense.setTitle("점심");
        expense.setAmount(new BigDecimal("12000"));
        expense.setSortOrder(0);
        txRepo.save(expense);

        var income = new com.pininicong.cashbook.domain.CbTransaction();
        income.setBook(LedgerBook.PERSONAL);
        income.setTxDate(date);
        income.setTxType(TxType.INCOME);
        income.setTitle("용돈");
        income.setAmount(new BigDecimal("50000"));
        income.setSortOrder(0);
        txRepo.save(income);

        var table = cashbookService.getDayTransactionTable(date, LedgerBook.PERSONAL);
        assertThat(table.tableName()).isEqualTo("cb_transaction");
        assertThat(table.txDate()).isEqualTo("2026-06-23");
        assertThat(table.querySql()).contains("tx_date = '2026-06-23'");
        assertThat(table.querySql()).contains("book = 'PERSONAL'");
        assertThat(table.count()).isEqualTo(2);
        assertThat(table.rows()).extracting("title").containsExactly("점심", "용돈");
        assertThat(table.rows()).extracting("txType").containsExactly("EXPENSE", "INCOME");
    }
}
