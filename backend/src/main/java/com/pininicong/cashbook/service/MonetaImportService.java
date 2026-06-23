package com.pininicong.cashbook.service;

import com.pininicong.cashbook.imports.MonetaCategoryCatalog;
import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryDto;
import com.pininicong.cashbook.dto.CategoryDto.CategoryListResponse;
import com.pininicong.cashbook.dto.MonetaImportResult;
import com.pininicong.cashbook.imports.MonetaHtmlReportParser;
import com.pininicong.cashbook.imports.MonetaHtmlReportParser.ParsedRow;
import com.pininicong.cashbook.imports.MonetaHtmlReportParser.ReportKind;
import com.pininicong.cashbook.repo.CbCategoryRepository;
import com.pininicong.cashbook.repo.CbFixedItemRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MonetaImportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CbTransactionRepository txRepo;
    private final CbCategoryRepository categoryRepo;
    private final CbFixedItemRepository fixedRepo;
    private final CategoryService categoryService;

    public MonetaImportService(
            CbTransactionRepository txRepo,
            CbCategoryRepository categoryRepo,
            CbFixedItemRepository fixedRepo,
            CategoryService categoryService) {
        this.txRepo = txRepo;
        this.categoryRepo = categoryRepo;
        this.fixedRepo = fixedRepo;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public CategoryListResponse listCategories(LedgerBook book) {
        return categoryService.listCategories(book);
    }

    @Transactional
    public MonetaImportResult importFromDirectory(Path root, boolean replaceExisting) {
        return importFromDirectory(root, replaceExisting, "20260101~20260630", LedgerBook.PERSONAL);
    }

    @Transactional
    public MonetaImportResult importFromDirectory(
            Path root, boolean replaceExisting, String periodKey) {
        return importFromDirectory(root, replaceExisting, periodKey, LedgerBook.PERSONAL);
    }

    @Transactional
    public MonetaImportResult importFromDirectory(
            Path root, boolean replaceExisting, String periodKey, LedgerBook book) {
        Path expense = root.resolve("지출");
        Path income = root.resolve("수입");
        Path savings = root.resolve("저축");
        Path insurance = root.resolve("보험");

        List<ParsedRow> all = new ArrayList<>();
        all.addAll(parseDir(expense, ReportKind.EXPENSE, periodKey));
        all.addAll(parseDir(income, ReportKind.INCOME, periodKey));
        all.addAll(parseDir(savings, ReportKind.SAVINGS, periodKey));
        all.addAll(parseDir(insurance, ReportKind.INSURANCE, periodKey));

        return persist(all, replaceExisting, book);
    }

    @Transactional
    public MonetaImportResult importFiles(Map<ReportKind, MultipartFile> files, boolean replaceExisting) {
        return importFiles(files, replaceExisting, LedgerBook.PERSONAL);
    }

    @Transactional
    public MonetaImportResult importFiles(
            Map<ReportKind, MultipartFile> files, boolean replaceExisting, LedgerBook book) {
        List<ParsedRow> all = new ArrayList<>();
        for (var e : files.entrySet()) {
            MultipartFile f = e.getValue();
            if (f == null || f.isEmpty()) continue;
            try (InputStream in = f.getInputStream()) {
                String html = new String(in.readAllBytes(), java.nio.charset.Charset.forName("EUC-KR"));
                all.addAll(MonetaHtmlReportParser.parseHtml(html, e.getKey()));
            } catch (IOException ex) {
                throw new IllegalArgumentException("업로드 파일 읽기 실패: " + ex.getMessage(), ex);
            }
        }
        return persist(all, replaceExisting, book);
    }

    @Transactional
    public int seedCategoriesFromClasspath() {
        return seedCategoriesFromClasspath(LedgerBook.PERSONAL);
    }

    @Transactional
    public int seedCategoriesFromClasspath(LedgerBook book) {
        int n = 0;
        for (var e : MonetaCategoryCatalog.NAMES.entrySet()) {
            if (e.getKey() == CategoryType.INCOME) {
                n += categoryService.ensureIncomeHierarchy(book);
            } else if (e.getKey() == CategoryType.EXPENSE && book == LedgerBook.PERSONAL) {
                n += categoryService.ensureExpenseHierarchy(book);
            } else {
                n += seedType(book, e.getKey(), e.getValue());
            }
        }
        return n;
    }

    private int seedType(LedgerBook book, CategoryType type, java.util.List<String> names) {
        if (names == null || names.isEmpty()) return 0;
        int added = 0;
        for (String name : names) {
            if (name.isBlank()) continue;
            if (!categoryRepo.existsByBookAndCategoryTypeAndName(book, type, name)) {
                categoryService.ensureMinorCategory(book, type, name);
                added++;
            }
        }
        return added;
    }

    private List<ParsedRow> parseDir(Path dir, ReportKind kind, String periodKey) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("폴더 없음: " + dir);
        }
        try {
            boolean allPeriods =
                    periodKey != null && ("*".equals(periodKey) || "all".equalsIgnoreCase(periodKey));
            String marker =
                    !allPeriods && periodKey != null && !periodKey.isBlank() ? "(" + periodKey + ")" : "";
            List<Path> files =
                    Files.list(dir)
                            .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".xls"))
                            .filter(p -> marker.isEmpty() || p.getFileName().toString().contains(marker))
                            .sorted()
                            .toList();
            if (files.isEmpty()) {
                throw new IllegalArgumentException("xls 파일 없음: " + dir + " 기간=" + periodKey);
            }
            if (!allPeriods && files.size() > 1) {
                Path pick = files.get(files.size() - 1);
                files = List.of(pick);
            }
            List<ParsedRow> rows = new ArrayList<>();
            for (Path f : files) {
                rows.addAll(MonetaHtmlReportParser.parse(f, kind));
            }
            return rows;
        } catch (IOException ex) {
            throw new IllegalArgumentException("폴더 읽기 실패: " + dir, ex);
        }
    }

    private MonetaImportResult persist(List<ParsedRow> rows, boolean replaceExisting, LedgerBook book) {
        if (replaceExisting) {
            txRepo.deleteByBook(book);
            fixedRepo.deleteByBook(book);
        }

        int exp = 0, inc = 0, sav = 0, ins = 0;
        Map<String, Integer> sortByDayType = new java.util.HashMap<>();

        rows.sort(Comparator.comparing(ParsedRow::date).thenComparing(r -> r.kind().ordinal()));

        Set<String> expenseCats = new HashSet<>();
        Set<String> incomeCats = new HashSet<>();
        Set<String> savingsNames = new HashSet<>();
        Set<String> insuranceNames = new HashSet<>();

        for (ParsedRow r : rows) {
            if (r.amount().compareTo(ZERO) == 0) continue;

            CbTransaction t = new CbTransaction();
            t.setBook(book);
            t.setTxDate(r.date());
            t.setTitle(r.title() != null ? r.title() : "");
            t.setAmount(r.amount());
            t.setCategory(r.category() != null ? r.category() : "");
            t.setCardName(r.cardName() != null ? r.cardName() : "");
            t.setRemarks(r.remarks() != null ? r.remarks() : "");

            switch (r.kind()) {
                case EXPENSE -> {
                    t.setTxType(TxType.EXPENSE);
                    expenseCats.add(r.category());
                    exp++;
                }
                case INCOME -> {
                    t.setTxType(TxType.INCOME);
                    incomeCats.add(r.category());
                    inc++;
                }
                case SAVINGS -> {
                    t.setTxType(TxType.SAVINGS);
                    t.setAccumulatedAmount(ZERO);
                    savingsNames.add(r.title());
                    sav++;
                }
                case INSURANCE -> {
                    t.setTxType(TxType.SAVINGS);
                    t.setCategory("보험");
                    t.setAccumulatedAmount(ZERO);
                    insuranceNames.add(r.title());
                    ins++;
                }
            }

            String sortKey = r.date() + ":" + t.getTxType();
            int order = sortByDayType.merge(sortKey, 1, Integer::sum) - 1;
            t.setSortOrder(order);
            txRepo.save(t);
        }

        int catAdded = registerCategories(book, expenseCats, incomeCats, savingsNames, insuranceNames);
        int fixedAdded = registerFixedItems(book, savingsNames, insuranceNames);

        return new MonetaImportResult(exp, inc, sav, ins, catAdded, fixedAdded);
    }

    private int registerCategories(
            LedgerBook book,
            Set<String> expenseCats,
            Set<String> incomeCats,
            Set<String> savingsNames,
            Set<String> insuranceNames) {
        int n = seedCategoriesFromClasspath(book);
        n += addCats(book, CategoryType.EXPENSE, expenseCats);
        n += addCats(book, CategoryType.INCOME, incomeCats);
        n += addCats(book, CategoryType.SAVINGS, savingsNames);
        n += addCats(book, CategoryType.INSURANCE, insuranceNames);
        return n;
    }

    private int addCats(LedgerBook book, CategoryType type, Set<String> names) {
        int added = 0;
        for (String name :
                names.stream().filter(s -> s != null && !s.isBlank()).sorted().collect(Collectors.toList())) {
            if (!categoryRepo.existsByBookAndCategoryTypeAndName(book, type, name)) {
                categoryService.ensureMinorCategory(book, type, name);
                added++;
            }
        }
        return added;
    }

    private int registerFixedItems(
            LedgerBook book, Set<String> savingsNames, Set<String> insuranceNames) {
        int order = 0;
        int added = 0;
        for (String name : savingsNames.stream().sorted().toList()) {
            if (name.isBlank()) continue;
            CbFixedItem f = new CbFixedItem();
            f.setBook(book);
            f.setTitle(name);
            f.setTxType(TxType.SAVINGS);
            f.setKind(com.pininicong.cashbook.domain.FixedKind.SAVINGS);
            f.setScheduleType(com.pininicong.cashbook.domain.FixedScheduleType.DAILY);
            f.setCategory("저축");
            f.setSortOrder(order++);
            fixedRepo.save(f);
            added++;
        }
        for (String name : insuranceNames.stream().sorted().toList()) {
            if (name.isBlank()) continue;
            CbFixedItem f = new CbFixedItem();
            f.setBook(book);
            f.setTitle(name);
            f.setTxType(TxType.SAVINGS);
            f.setKind(com.pininicong.cashbook.domain.FixedKind.SAVINGS);
            f.setScheduleType(com.pininicong.cashbook.domain.FixedScheduleType.DAILY);
            f.setCategory("보험");
            f.setSortOrder(order++);
            fixedRepo.save(f);
            added++;
        }
        return added;
    }
}
