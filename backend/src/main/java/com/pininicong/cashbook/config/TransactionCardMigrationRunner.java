package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.service.TransactionCardSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** legacy card_name → card_product_id 백필 후 name 컬럼 제거. */
@Component
@Order(11)
public class TransactionCardMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TransactionCardMigrationRunner.class);

    private final JdbcTemplate jdbc;
    private final TransactionCardSupport cardSupport;

    public TransactionCardMigrationRunner(JdbcTemplate jdbc, TransactionCardSupport cardSupport) {
        this.jdbc = jdbc;
        this.cardSupport = cardSupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("cb_transaction")) {
            return;
        }
        backfillCardProductId();
        dropLegacyColumn("card_name");
    }

    private void backfillCardProductId() {
        if (!columnExists("cb_transaction", "card_name")
                || !columnExists("cb_transaction", "card_product_id")) {
            return;
        }
        var rows =
                jdbc.queryForList(
                        """
                        SELECT id, book, card_name AS legacy_name, card_product_id
                        FROM cb_transaction
                        WHERE TRIM(COALESCE(card_name, '')) <> '' AND card_product_id IS NULL
                        """);
        int updated = 0;
        for (var row : rows) {
            Long txId = ((Number) row.get("ID")).longValue();
            String bookCode = String.valueOf(row.get("BOOK"));
            String legacyName = String.valueOf(row.get("LEGACY_NAME")).trim();
            LedgerBook book = LedgerBook.valueOf(bookCode);
            var resolved = cardSupport.findCardIdByName(book, legacyName);
            if (resolved.isEmpty()) {
                resolved =
                        cardSupport
                                .resolveCard(book, null, legacyName)
                                .map(TransactionCardSupport.ResolvedCard::id);
            }
            if (resolved.isEmpty()) {
                continue;
            }
            jdbc.update(
                    "UPDATE cb_transaction SET card_product_id = ? WHERE id = ?",
                    resolved.get(),
                    txId);
            updated++;
        }
        if (updated > 0) {
            log.info("cb_transaction card_name → card_product_id 백필 {}건", updated);
        }
    }

    private void dropLegacyColumn(String column) {
        if (!columnExists("cb_transaction", column)) {
            return;
        }
        try {
            jdbc.execute("ALTER TABLE cb_transaction DROP COLUMN " + column);
            log.info("cb_transaction legacy column dropped: {}", column);
        } catch (Exception e) {
            log.warn("cb_transaction column drop skipped ({}): {}", column, e.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Integer count =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                        WHERE UPPER(TABLE_NAME) = UPPER(?)
                        """,
                        Integer.class,
                        table);
        return count != null && count > 0;
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
