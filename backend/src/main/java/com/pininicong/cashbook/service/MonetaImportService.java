package com.pininicong.cashbook.service;

import com.pininicong.cashbook.imports.HouseholdIncomeTitleCategoryMapper;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private final TransactionCategorySupport categorySupport;
    private final TransactionCardSupport cardSupport;
    private final TransactionSavingsProductSupport savingsProductSupport;

    public MonetaImportService(
            CbTransactionRepository txRepo,
            CbCategoryRepository categoryRepo,
            CbFixedItemRepository fixedRepo,
            CategoryService categoryService,
            TransactionCategorySupport categorySupport,
            TransactionCardSupport cardSupport,
            TransactionSavingsProductSupport savingsProductSupport) {
        this.txRepo = txRepo;
        this.categoryRepo = categoryRepo;
        this.fixedRepo = fixedRepo;
        this.categoryService = categoryService;
        this.categorySupport = categorySupport;
        this.cardSupport = cardSupport;
        this.savingsProductSupport = savingsProductSupport;
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
            switch (r.kind()) {
                case EXPENSE -> expenseCats.add(r.category());
                case INCOME -> incomeCats.add(r.category());
                case SAVINGS -> savingsNames.add(r.title());
                case INSURANCE -> insuranceNames.add(r.title());
                default -> {}
            }
        }

        int catAdded = registerCategories(book, expenseCats, incomeCats, savingsNames, insuranceNames);

        for (ParsedRow r : rows) {
            if (r.amount().compareTo(ZERO) == 0) continue;

            CbTransaction t = new CbTransaction();
            t.setBook(book);
            t.setTxDate(r.date());
            t.setTitle(r.title() != null ? r.title() : "");
            t.setAmount(r.amount());
            t.setRemarks(r.remarks() != null ? r.remarks() : "");

            switch (r.kind()) {
                case EXPENSE -> {
                    t.setTxType(TxType.EXPENSE);
                    exp++;
                }
                case INCOME -> {
                    t.setTxType(TxType.INCOME);
                    inc++;
                }
                case SAVINGS -> {
                    t.setTxType(TxType.SAVINGS);
                    t.setAccumulatedAmount(ZERO);
                    sav++;
                }
                case INSURANCE -> {
                    t.setTxType(TxType.SAVINGS);
                    t.setAccumulatedAmount(ZERO);
                    ins++;
                }
            }

            if (r.kind() == ReportKind.INSURANCE) {
                applyCategoryId(t, book, TxType.SAVINGS, "보험");
            } else {
                applyCategoryId(t, book, t.getTxType(), r.category());
            }
            if (t.getTxType() == TxType.EXPENSE) {
                cardSupport
                        .resolveCard(book, null, r.cardName())
                        .ifPresent(resolved -> t.setCardProductId(resolved.id()));
            }
            if (t.getTxType() == TxType.SAVINGS) {
                savingsProductSupport.applySavingsProduct(t, null, t.getTitle());
            }

            String sortKey = r.date() + ":" + t.getTxType();
            int order = sortByDayType.merge(sortKey, 1, Integer::sum) - 1;
            t.setSortOrder(order);
            txRepo.save(t);
        }

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

    private void applyCategoryId(
            CbTransaction t, LedgerBook book, TxType txType, String categoryName) {
        CategoryType type = TransactionCategorySupport.categoryTypeFor(txType);
        categorySupport
                .resolveMinor(book, type, null, categoryName)
                .ifPresent(resolved -> t.setCategoryId(resolved.id()));
    }

    /**
     * category_id 가 비어 있는 거래를 모네타 HTML(일자·항목·금액)과 대조해 분류를 연결합니다.
     * import 폴더가 없거나 매칭되지 않으면 0을 반환합니다.
     */
    @Transactional
    public int backfillMissingCategoryIdsFromMoneta(Path root, LedgerBook book) {
        if (root == null || !Files.isDirectory(root)) {
            return 0;
        }
        try {
            List<ParsedRow> rows = new ArrayList<>();
            Path expense = root.resolve("지출");
            Path income = root.resolve("수입");
            if (Files.isDirectory(expense)) {
                rows.addAll(parseDirQuiet(expense, ReportKind.EXPENSE));
            }
            if (Files.isDirectory(income)) {
                rows.addAll(parseDirQuiet(income, ReportKind.INCOME));
            }
            if (rows.isEmpty()) {
                return 0;
            }
            return backfillMissingCategoryIds(book, rows);
        } catch (Exception ex) {
            return 0;
        }
    }

    /** 가계 수입: 제목 패턴으로 category_id 를 보강·교정합니다. */
    @Transactional
    public int backfillHouseholdIncomeCategoryIdsFromTitles() {
        int updated = 0;
        for (CbTransaction tx :
                txRepo.findByBookAndTxTypeOrderByTxDateDescIdDesc(
                        LedgerBook.HOUSEHOLD, TxType.INCOME)) {
            Optional<String> categoryName =
                    HouseholdIncomeTitleCategoryMapper.categoryForTitle(tx.getTitle());
            if (categoryName.isEmpty()) {
                continue;
            }
            String expected = categoryName.get();
            String current =
                    categorySupport.name(LedgerBook.HOUSEHOLD, tx.getCategoryId());
            if (expected.equals(current)) {
                continue;
            }
            if (applyResolvedCategoryId(tx, LedgerBook.HOUSEHOLD, TxType.INCOME, expected)) {
                updated++;
            }
        }
        return updated;
    }

    private int backfillMissingCategoryIds(LedgerBook book, List<ParsedRow> rows) {
        Map<TxMatchKey, String> categoryByKey = new LinkedHashMap<>();
        for (ParsedRow row : rows) {
            if (row.amount().compareTo(ZERO) == 0) {
                continue;
            }
            TxType txType =
                    switch (row.kind()) {
                        case EXPENSE -> TxType.EXPENSE;
                        case INCOME -> TxType.INCOME;
                        case SAVINGS, INSURANCE -> TxType.SAVINGS;
                    };
            if (txType == TxType.SAVINGS) {
                continue;
            }
            String categoryName =
                    row.kind() == ReportKind.INSURANCE ? "보험" : normalize(row.category());
            if (categoryName.isEmpty()) {
                continue;
            }
            categoryByKey.put(
                    new TxMatchKey(row.date(), normalize(row.title()), row.amount(), txType),
                    categoryName);
        }

        int updated = 0;
        for (CbTransaction tx : txRepo.findByBookAndCategoryIdIsNull(book)) {
            String categoryName =
                    categoryByKey.get(
                            new TxMatchKey(
                                    tx.getTxDate(),
                                    normalize(tx.getTitle()),
                                    tx.getAmount(),
                                    tx.getTxType()));
            if (categoryName == null || categoryName.isEmpty()) {
                continue;
            }
            if (applyResolvedCategoryId(tx, book, tx.getTxType(), categoryName)) {
                updated++;
            }
        }
        return updated;
    }

    private boolean applyResolvedCategoryId(
            CbTransaction tx, LedgerBook book, TxType txType, String categoryName) {
        CategoryType type = TransactionCategorySupport.categoryTypeFor(txType);
        var resolved = categorySupport.resolveMinor(book, type, null, categoryName);
        if (resolved.isEmpty()) {
            return false;
        }
        tx.setCategoryId(resolved.get().id());
        txRepo.save(tx);
        return true;
    }

    private List<ParsedRow> parseDirQuiet(Path dir, ReportKind kind) {
        try {
            return parseDir(dir, kind, "*");
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }

    private record TxMatchKey(LocalDate date, String title, BigDecimal amount, TxType txType) {}
}
