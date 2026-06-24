package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.service.TransactionSavingsProductSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** cb_transaction.title → savings_product_id 백필. */
@Component
@Order(13)
public class TransactionSavingsProductMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TransactionSavingsProductMigrationRunner.class);

    private final JdbcTemplate jdbc;
    private final TransactionSavingsProductSupport savingsProductSupport;

    public TransactionSavingsProductMigrationRunner(
            JdbcTemplate jdbc, TransactionSavingsProductSupport savingsProductSupport) {
        this.jdbc = jdbc;
        this.savingsProductSupport = savingsProductSupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!columnExists("cb_transaction", "savings_product_id")) {
            return;
        }
        backfillSavingsProductId();
    }

    private void backfillSavingsProductId() {
        var rows =
                jdbc.queryForList(
                        """
                        SELECT id, book, title, savings_product_id
                        FROM cb_transaction
                        WHERE tx_type = 'SAVINGS' AND savings_product_id IS NULL
                        """);
        int updated = 0;
        for (var row : rows) {
            Long txId = ((Number) row.get("ID")).longValue();
            String bookCode = String.valueOf(row.get("BOOK"));
            String title = String.valueOf(row.get("TITLE")).trim();
            try {
                LedgerBook book = LedgerBook.valueOf(bookCode);
                var resolved = savingsProductSupport.resolveProduct(book, null, title);
                if (resolved.isEmpty()) {
                    continue;
                }
                jdbc.update(
                        "UPDATE cb_transaction SET savings_product_id = ? WHERE id = ?",
                        resolved.get().id(),
                        txId);
                updated++;
            } catch (Exception e) {
                log.warn("savings_product_id 백필 생략 txId={} title={}: {}", txId, title, e.getMessage());
            }
        }
        if (updated > 0) {
            log.info("cb_transaction title → savings_product_id 백필 {}건", updated);
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)
                        """,
                        Integer.class,
                        table,
                        column);
        return count != null && count > 0;
    }
}
