package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.service.MonetaImportService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * category_id 미연결 거래를 모네타 HTML·제목 규칙으로 보강하고, 동명 소분류 ID를 대표 ID로 통합합니다.
 */
@Component
@Order(12)
public class TransactionCategoryRelinkRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TransactionCategoryRelinkRunner.class);

    private final TransactionCategoryRelinkSupport relinkSupport;
    private final MonetaImportService importService;

    @Value("${pininicong.import.dir:}")
    private String importDir;

    @Value("${pininicong.import.personal-dir:}")
    private String personalImportDir;

    @Value("${pininicong.import.household-dir:}")
    private String householdImportDir;

    public TransactionCategoryRelinkRunner(
            TransactionCategoryRelinkSupport relinkSupport, MonetaImportService importService) {
        this.relinkSupport = relinkSupport;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int fromLegacy = relinkSupport.backfillFromLegacyCategoryColumn();
        int fromMoneta = backfillFromMonetaDirs();
        int fromTitles = importService.backfillHouseholdIncomeCategoryIdsFromTitles();
        int consolidated = relinkSupport.consolidateDuplicateMinorLinks();
        if (fromLegacy > 0 || fromMoneta > 0 || fromTitles > 0 || consolidated > 0) {
            log.info(
                    "거래 분류 재연결: legacy {}건, 모네타 {}건, 제목규칙 {}건, 동명 ID 통합 {}건",
                    fromLegacy,
                    fromMoneta,
                    fromTitles,
                    consolidated);
        }
    }

    private int backfillFromMonetaDirs() {
        int updated = 0;
        updated += backfillBook(LedgerBook.PERSONAL, resolveDir(personalImportDir, importDir));
        updated += backfillBook(LedgerBook.HOUSEHOLD, resolveDir(householdImportDir, ""));
        return updated;
    }

    private int backfillBook(LedgerBook book, Path root) {
        if (root == null) {
            return 0;
        }
        return importService.backfillMissingCategoryIdsFromMoneta(root, book);
    }

    private static Path resolveDir(String primary, String fallback) {
        for (String candidate : new String[] {primary, fallback}) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Path path = Path.of(candidate.trim());
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return null;
    }
}
