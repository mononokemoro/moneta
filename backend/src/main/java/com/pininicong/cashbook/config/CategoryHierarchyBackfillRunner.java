package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 기존 평면 분류를 대분류/소분류 계층으로 백필합니다. */
@Component
@Order(2)
public class CategoryHierarchyBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryHierarchyBackfillRunner.class);

    private final CategoryService categoryService;

    public CategoryHierarchyBackfillRunner(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (LedgerBook book : LedgerBook.values()) {
            int flat = categoryService.migrateFlatToHierarchy(book);
            if (flat > 0) {
                log.info("{} 장부 분류 계층 백필 {}건", book.label(), flat);
            }
            int income = categoryService.migrateIncomeHierarchy(book);
            if (income > 0) {
                log.info("{} 장부 수입 분류 계층 정리 {}건", book.label(), income);
            }
            int expense = categoryService.migrateExpenseHierarchy(book);
            if (expense > 0) {
                log.info("{} 장부 지출 분류 계층 정리 {}건", book.label(), expense);
            }
        }
    }
}
