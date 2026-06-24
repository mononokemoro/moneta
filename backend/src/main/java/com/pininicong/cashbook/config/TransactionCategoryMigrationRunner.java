package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.service.TransactionCategorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** legacy category 문자열 → category_id 백필 후 name 컬럼 제거. */
@Component
@Order(10)
public class TransactionCategoryMigrationRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionCategoryMigrationRunner.class);

    private final JdbcTemplate jdbc;
    private final TransactionCategorySupport categorySupport;

    public TransactionCategoryMigrationRunner(
            JdbcTemplate jdbc, TransactionCategorySupport categorySupport) {
        this.jdbc = jdbc;
        this.categorySupport = categorySupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("cb_transaction")) {
            return;
        }
        backfillCategoryColumn("category", "category_id", false);
        backfillCategoryColumn("household_category", "household_category_id", true);
        dropLegacyColumn("category");
        dropLegacyColumn("household_category");
    }

    private void backfillCategoryColumn(
            String legacyColumn, String idColumn, boolean householdOnPersonal) {
        if (!columnExists("cb_transaction", legacyColumn)
                || !columnExists("cb_transaction", idColumn)) {
            return;
        }
        var rows =
                jdbc.queryForList(
                        """
                        SELECT id, book, tx_type, %s AS legacy_name, %s AS category_id
                        FROM cb_transaction
                        WHERE TRIM(COALESCE(%s, '')) <> '' AND %s IS NULL
                        """
                                .formatted(legacyColumn, idColumn, legacyColumn, idColumn));
        int updated = 0;
        for (var row : rows) {
            Long txId = ((Number) row.get("ID")).longValue();
            String bookCode = String.valueOf(row.get("BOOK"));
            String txTypeCode = String.valueOf(row.get("TX_TYPE"));
            String legacyName = String.valueOf(row.get("LEGACY_NAME")).trim();
            LedgerBook book = LedgerBook.valueOf(bookCode);
            TxType txType = TxType.valueOf(txTypeCode);
            CategoryType catType = TransactionCategorySupport.categoryTypeFor(txType);
            LedgerBook lookupBook = householdOnPersonal ? LedgerBook.HOUSEHOLD : book;
            var resolved = categorySupport.findMinorIdByName(lookupBook, catType, legacyName);
            if (resolved.isEmpty() && householdOnPersonal) {
                resolved = categorySupport.findMinorIdByName(book, catType, legacyName);
            }
            if (resolved.isEmpty()) {
                continue;
            }
            jdbc.update(
                    "UPDATE cb_transaction SET %s = ? WHERE id = ?".formatted(idColumn),
                    resolved.get(),
                    txId);
            updated++;
        }
        if (updated > 0) {
            log.info("cb_transaction {} → {} 백필 {}건", legacyColumn, idColumn, updated);
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
