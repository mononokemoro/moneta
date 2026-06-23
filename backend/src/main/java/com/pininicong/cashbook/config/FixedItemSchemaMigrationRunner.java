package com.pininicong.cashbook.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 기존 cb_fixed_item 행에 고정등록 기본값을 채웁니다. */
@Component
@Order(2)
public class FixedItemSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FixedItemSchemaMigrationRunner.class);

    private final JdbcTemplate jdbc;

    public FixedItemSchemaMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumns();
        backfillDefaults();
        backfillKindFromTxType();
        backfillDailyScheduleForLegacyRows();
    }

    /** Hibernate ddl-auto가 누락한 컬럼을 보강합니다 (구버전 DB 호환). */
    private void ensureColumns() {
        addColumnIfMissing("kind", "varchar(16)");
        addColumnIfMissing("schedule_type", "varchar(16)");
        addColumnIfMissing("day_of_month", "integer");
        addColumnIfMissing("last_day_of_month", "boolean");
        addColumnIfMissing("holiday_adjust", "varchar(16)");
        addColumnIfMissing("period_type", "varchar(16)");
        addColumnIfMissing("period_start", "date");
        addColumnIfMissing("period_end", "date");
        addColumnIfMissing("remarks", "varchar(500)");
        addColumnIfMissing("interest_amount", "decimal(19,2)");
        addColumnIfMissing("payment_method", "varchar(40)");
    }

    private void addColumnIfMissing(String column, String type) {
        try {
            jdbc.execute(
                    "ALTER TABLE cb_fixed_item ADD COLUMN IF NOT EXISTS "
                            + column
                            + " "
                            + type);
        } catch (Exception e) {
            log.warn("cb_fixed_item column ensure skipped ({}): {}", column, e.getMessage());
        }
    }

    private void backfillDefaults() {
        try {
            int updated =
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET holiday_adjust = 'NONE'
                            WHERE holiday_adjust IS NULL
                            """);
            updated +=
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET period_type = 'CONTINUOUS'
                            WHERE period_type IS NULL
                            """);
            updated +=
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET last_day_of_month = FALSE
                            WHERE last_day_of_month IS NULL
                            """);
            updated +=
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET interest_amount = 0
                            WHERE interest_amount IS NULL
                            """);
            updated +=
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET payment_method = '현금'
                            WHERE payment_method IS NULL
                            """);
            updated +=
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET remarks = ''
                            WHERE remarks IS NULL
                            """);
            updated +=
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET schedule_type = 'MONTHLY'
                            WHERE schedule_type IS NULL
                            """);
            if (updated > 0) {
                log.info("cb_fixed_item defaults backfilled: {}", updated);
            }
        } catch (Exception e) {
            log.debug("cb_fixed_item defaults backfill skipped: {}", e.getMessage());
        }
    }

    private void backfillKindFromTxType() {
        try {
            int updated =
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET kind = CASE tx_type
                                WHEN 'INCOME' THEN 'INCOME'
                                WHEN 'SAVINGS' THEN 'SAVINGS'
                                ELSE 'EXPENSE'
                            END
                            WHERE kind IS NULL
                               OR (kind = 'EXPENSE' AND tx_type IN ('INCOME', 'SAVINGS'))
                               OR (kind = 'INCOME' AND tx_type <> 'INCOME')
                               OR (kind = 'SAVINGS' AND tx_type <> 'SAVINGS')
                            """);
            if (updated > 0) {
                log.info("cb_fixed_item kind backfilled: {}", updated);
            }
        } catch (Exception e) {
            log.debug("cb_fixed_item kind backfill skipped: {}", e.getMessage());
        }
    }

    private void backfillDailyScheduleForLegacyRows() {
        try {
            int updated =
                    jdbc.update(
                            """
                            UPDATE cb_fixed_item
                            SET schedule_type = 'DAILY'
                            WHERE day_of_month IS NULL
                              AND COALESCE(last_day_of_month, FALSE) = FALSE
                              AND schedule_type = 'MONTHLY'
                            """);
            if (updated > 0) {
                log.info("cb_fixed_item legacy schedule backfilled: {}", updated);
            }
        } catch (Exception e) {
            log.debug("cb_fixed_item schedule backfill skipped: {}", e.getMessage());
        }
    }
}
