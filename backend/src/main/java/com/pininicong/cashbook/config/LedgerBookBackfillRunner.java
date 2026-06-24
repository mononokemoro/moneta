package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 기존 단일 장부 DB에 book 컬럼이 추가된 뒤 PERSONAL 로 백필합니다. */
@Component
@Order(1)
public class LedgerBookBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LedgerBookBackfillRunner.class);

    private final JdbcTemplate jdbc;

    public LedgerBookBackfillRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureBookColumn("cb_transaction");
        ensureHouseholdCategoryColumn();
        ensureBookColumn("cb_category");
        ensureBookColumn("cb_fixed_item");
        ensureBookColumn("cb_monthly_budget");
        ensureBookColumn("cb_daily_sheet");
        migrateLegacyCashBalance();
        backfillColumn("cb_transaction", "book");
        backfillColumn("cb_category", "book");
        backfillColumn("cb_fixed_item", "book");
        backfillColumn("cb_monthly_budget", "book");
        backfillColumn("cb_daily_sheet", "book");
        backfillColumn("cb_cash_balance", "book");
        migrateCategoryUniqueConstraint();
    }

    /** 장부별 카테고리: (category_type, name) 단일 unique → (book, category_type, name) */
    private void migrateCategoryUniqueConstraint() {
        if (!tableExists("cb_category") || !columnExists("cb_category", "book")) {
            return;
        }
        List<String> legacy =
                jdbc.query(
                        """
                        SELECT tc.CONSTRAINT_NAME
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                        WHERE UPPER(tc.TABLE_NAME) = 'CB_CATEGORY'
                          AND tc.CONSTRAINT_TYPE = 'UNIQUE'
                          AND tc.CONSTRAINT_NAME NOT IN (
                            SELECT tc2.CONSTRAINT_NAME
                            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc2
                            INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                              ON tc2.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                            WHERE UPPER(tc2.TABLE_NAME) = 'CB_CATEGORY'
                              AND tc2.CONSTRAINT_TYPE = 'UNIQUE'
                              AND UPPER(kcu.COLUMN_NAME) = 'BOOK'
                          )
                        """,
                        (rs, rowNum) -> rs.getString(1));
        for (String name : legacy) {
            try {
                jdbc.execute("ALTER TABLE cb_category DROP CONSTRAINT " + name);
                log.info("cb_category 레거시 unique 제약 제거: {}", name);
            } catch (Exception e) {
                log.warn("cb_category unique 제약 제거 실패 {}: {}", name, e.getMessage());
            }
        }
        List<Integer> bookScoped =
                jdbc.query(
                        """
                        SELECT COUNT(DISTINCT UPPER(kcu.COLUMN_NAME))
                        FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                        INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                          ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                        WHERE UPPER(tc.TABLE_NAME) = 'CB_CATEGORY'
                          AND tc.CONSTRAINT_TYPE = 'UNIQUE'
                          AND UPPER(kcu.COLUMN_NAME) IN ('BOOK', 'CATEGORY_TYPE', 'NAME')
                        GROUP BY tc.CONSTRAINT_NAME
                        HAVING COUNT(DISTINCT UPPER(kcu.COLUMN_NAME)) = 3
                        """,
                        (rs, rowNum) -> rs.getInt(1));
        if (bookScoped.isEmpty()) {
            try {
                jdbc.execute(
                        "ALTER TABLE cb_category ADD CONSTRAINT uk_cb_category_book_type_name"
                                + " UNIQUE (book, category_type, name)");
                log.info("cb_category (book, category_type, name) unique 추가");
            } catch (Exception e) {
                log.warn("cb_category unique 추가 실패: {}", e.getMessage());
            }
        }
    }

    private void ensureBookColumn(String table) {
        if (!tableExists(table) || columnExists(table, "book")) {
            return;
        }
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN book VARCHAR(16)");
        jdbc.update(
                "UPDATE " + table + " SET book = ? WHERE book IS NULL", LedgerBook.PERSONAL.name());
        log.info("{}.book 컬럼 추가 및 PERSONAL 백필", table);
    }

    private void ensureHouseholdCategoryColumn() {
        if (!tableExists("cb_transaction") || columnExists("cb_transaction", "household_category")) {
            return;
        }
        jdbc.execute("ALTER TABLE cb_transaction ADD COLUMN household_category VARCHAR(80)");
        jdbc.update("UPDATE cb_transaction SET household_category = '' WHERE household_category IS NULL");
        log.info("cb_transaction.household_category 컬럼 추가");
    }

    private void backfillColumn(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        int updated =
                jdbc.update(
                        "UPDATE "
                                + table
                                + " SET "
                                + column
                                + " = ? WHERE "
                                + column
                                + " IS NULL",
                        LedgerBook.PERSONAL.name());
        if (updated > 0) {
            log.info("{}.{} NULL → PERSONAL {}건", table, column, updated);
        }
    }

    private void migrateLegacyCashBalance() {
        if (!tableExists("cb_cash_balance")) {
            return;
        }
        boolean hasBook = columnExists("cb_cash_balance", "book");
        boolean hasLegacyId = columnExists("cb_cash_balance", "id");
        if (!hasBook && hasLegacyId) {
            jdbc.execute("ALTER TABLE cb_cash_balance ADD COLUMN book VARCHAR(16)");
            jdbc.update(
                    "UPDATE cb_cash_balance SET book = ? WHERE book IS NULL",
                    LedgerBook.PERSONAL.name());
            dropColumnIfExists("cb_cash_balance", "id");
            log.info("cb_cash_balance: legacy id → book 마이그레이션");
            return;
        }
        if (!hasBook) {
            return;
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM cb_cash_balance", Integer.class) == 0) {
            return;
        }
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cb_cash_balance WHERE book = ?",
                        Integer.class,
                        LedgerBook.PERSONAL.name())
                > 0) {
            return;
        }
        if (hasLegacyId) {
            jdbc.update(
                    """
                    INSERT INTO cb_cash_balance (book, amount)
                    SELECT ?, amount FROM cb_cash_balance WHERE id = 1
                    """,
                    LedgerBook.PERSONAL.name());
            jdbc.update("DELETE FROM cb_cash_balance WHERE id IS NOT NULL AND book IS NULL");
            log.info("cb_cash_balance 레거시 id=1 → PERSONAL 마이그레이션");
        }
    }

    private boolean tableExists(String table) {
        Integer n =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                        WHERE UPPER(TABLE_NAME) = UPPER(?)
                        """,
                        Integer.class,
                        table);
        return n != null && n > 0;
    }

    private void dropColumnIfExists(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        try {
            jdbc.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
        } catch (Exception e) {
            log.warn("{}.{} 컬럼 제거 실패: {}", table, column, e.getMessage());
        }
    }

    private boolean columnExists(String table, String column) {
        Integer n =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)
                        """,
                        Integer.class,
                        table,
                        column);
        return n != null && n > 0;
    }
}
