package com.pininicong.cashbook.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** cb_category 고유 제약을 (book, type, name, tier) 로 맞춥니다. */
@Component
@Order(1)
public class CategorySchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySchemaMigrationRunner.class);

    private final JdbcTemplate jdbc;

    public CategorySchemaMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        dropLegacyUniqueIndex("UKE19JR5R8L6NJIMJULPWM7A9KV");
        dropLegacyUniqueIndex("UK_CB_CATEGORY_BOOK_TYPE_NAME");
        dropCategoryUniqueWithoutParent();
    }

    /** (book, type, name, tier) 제약을 parent_id 포함 제약으로 교체합니다. */
    private void dropCategoryUniqueWithoutParent() {
        try {
            var names =
                    jdbc.queryForList(
                            """
                            SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                            WHERE TABLE_NAME = 'CB_CATEGORY' AND CONSTRAINT_TYPE = 'UNIQUE'
                            """,
                            String.class);
            for (String name : names) {
                var cols =
                        jdbc.queryForList(
                                """
                                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                                WHERE TABLE_NAME = 'CB_CATEGORY' AND CONSTRAINT_NAME = ?
                                ORDER BY ORDINAL_POSITION
                                """,
                                String.class,
                                name);
                if (cols.size() == 4
                        && cols.containsAll(
                                List.of("BOOK", "CATEGORY_TYPE", "NAME", "TIER"))) {
                    dropLegacyUniqueIndex(name);
                }
            }
        } catch (Exception e) {
            log.debug("cb_category parent-scoped unique migration skipped: {}", e.getMessage());
        }
    }

    private void dropLegacyUniqueIndex(String name) {
        try {
            jdbc.execute("ALTER TABLE cb_category DROP CONSTRAINT IF EXISTS " + name);
            log.info("cb_category legacy constraint dropped: {}", name);
        } catch (Exception e) {
            log.debug("cb_category constraint drop skipped ({}): {}", name, e.getMessage());
        }
    }
}
