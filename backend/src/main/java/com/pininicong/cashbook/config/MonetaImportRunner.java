package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.service.MonetaImportService;
import com.pininicong.cashbook.support.LedgerBookParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** PININICONG_IMPORT_DIR 환경변수(또는 pininicong.import.dir)가 있으면 기동 시 자동 임포트 */
@Component
@Order(100)
@ConditionalOnProperty(name = "pininicong.import.on-startup", havingValue = "true")
public class MonetaImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MonetaImportRunner.class);

    private final MonetaImportService importService;

    @Value("${pininicong.import.dir:}")
    private String importDir;

    @Value("${pininicong.import.period:20260101~20260630}")
    private String importPeriod;

    @Value("${pininicong.import.replace:true}")
    private boolean replace;

    @Value("${pininicong.import.book:PERSONAL}")
    private String importBook;

    public MonetaImportRunner(MonetaImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (importDir == null || importDir.isBlank()) {
            log.warn("pininicong.import.on-startup=true 이지만 pininicong.import.dir 가 비어 있습니다.");
            return;
        }
        Path root = Path.of(importDir);
        if (!Files.isDirectory(root)) {
            log.warn("임포트 경로가 없습니다: {}", root);
            return;
        }
        log.info("모네타 보고서 임포트 시작: {} (book={})", root, importBook);
        LedgerBook book = LedgerBookParser.parse(importBook);
        var result = importService.importFromDirectory(root, replace, importPeriod, book);
        log.info(
                "임포트 완료 — 지출 {} / 수입 {} / 저축 {} / 보험 {} / 분류 {} / 고정 {}",
                result.expensesImported(),
                result.incomesImported(),
                result.savingsImported(),
                result.insuranceImported(),
                result.categoriesRegistered(),
                result.fixedItemsRegistered());
    }
}
