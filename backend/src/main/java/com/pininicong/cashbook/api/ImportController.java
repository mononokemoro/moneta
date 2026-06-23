package com.pininicong.cashbook.api;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.dto.MonetaImportResult;
import com.pininicong.cashbook.imports.MonetaHtmlReportParser.ReportKind;
import com.pininicong.cashbook.service.MonetaImportService;
import com.pininicong.cashbook.support.LedgerBookParser;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImportController {

    private final MonetaImportService importService;

    @Value("${pininicong.import.dir:}")
    private String importDir;

    public ImportController(MonetaImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/categories/seed")
    public int seedCategories(@RequestParam(defaultValue = "PERSONAL") String book) {
        return importService.seedCategoriesFromClasspath(LedgerBookParser.parse(book));
    }

    @PostMapping("/import/moneta-reports")
    public MonetaImportResult importReports(
            @RequestParam(defaultValue = "true") boolean replace,
            @RequestParam(defaultValue = "PERSONAL") String book,
            @RequestParam(required = false) MultipartFile expense,
            @RequestParam(required = false) MultipartFile income,
            @RequestParam(required = false) MultipartFile savings,
            @RequestParam(required = false) MultipartFile insurance) {
        Map<ReportKind, MultipartFile> files = new EnumMap<>(ReportKind.class);
        files.put(ReportKind.EXPENSE, expense);
        files.put(ReportKind.INCOME, income);
        files.put(ReportKind.SAVINGS, savings);
        files.put(ReportKind.INSURANCE, insurance);
        return importService.importFiles(files, replace, LedgerBookParser.parse(book));
    }

    @PostMapping("/import/moneta-directory")
    public MonetaImportResult importDirectory(
            @RequestParam(required = false) String dir,
            @RequestParam(defaultValue = "20260101~20260630") String period,
            @RequestParam(defaultValue = "true") boolean replace,
            @RequestParam(defaultValue = "PERSONAL") String book) {
        String path = dir != null && !dir.isBlank() ? dir : importDir;
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("dir 파라미터 또는 pininicong.import.dir 설정이 필요합니다.");
        }
        return importService.importFromDirectory(
                Path.of(path), replace, period, LedgerBookParser.parse(book));
    }
}
