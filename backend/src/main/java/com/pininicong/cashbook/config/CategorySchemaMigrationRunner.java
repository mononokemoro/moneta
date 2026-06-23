package com.pininicong.cashbook.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
