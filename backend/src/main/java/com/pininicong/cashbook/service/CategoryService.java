package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryCreateRequest;
import com.pininicong.cashbook.dto.CategoryDto;
import com.pininicong.cashbook.dto.CategoryDto.CategoryGroupDto;
import com.pininicong.cashbook.dto.CategoryDto.CategoryListResponse;
import com.pininicong.cashbook.dto.CategoryReorderItem;
import com.pininicong.cashbook.dto.CategoryPreferencesRequest;
import com.pininicong.cashbook.dto.CategoryPreferencesRequest.CategoryPreferenceItem;
import com.pininicong.cashbook.dto.CategoryReorderRequest;
import com.pininicong.cashbook.dto.CategoryUpdateRequest;
import com.pininicong.cashbook.imports.HouseholdExpenseCategoryCatalog;
import com.pininicong.cashbook.imports.HouseholdIncomeCategoryCatalog;
import com.pininicong.cashbook.imports.MonetaIncomeCategoryCatalog;
import com.pininicong.cashbook.imports.MonetaExpenseCategoryCatalog;
import com.pininicong.cashbook.repo.CbCategoryKeywordRepository;
import com.pininicong.cashbook.repo.CbCategoryRepository;
import com.pininicong.cashbook.repo.CbFixedItemRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.dto.CategoryDto.CategoryTransactionRowDto;
import com.pininicong.cashbook.dto.CategoryDto.CategoryTransactionTableResponse;
import com.pininicong.cashbook.dto.CategoryDto.CategoryTransactionsResponse;
import com.pininicong.cashbook.dto.TransactionTableRowDto;
import com.pininicong.cashbook.support.TransactionTableSupport;
import com.pininicong.cashbook.domain.CbTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final String DEFAULT_MAJOR = "기타";

    private final CbCategoryRepository categoryRepo;
    private final CbTransactionRepository txRepo;
    private final CbFixedItemRepository fixedRepo;
    private final CbCategoryKeywordRepository keywordRepo;
    private final TransactionCardSupport cardSupport;

    public CategoryService(
            CbCategoryRepository categoryRepo,
            CbTransactionRepository txRepo,
            CbFixedItemRepository fixedRepo,
            CbCategoryKeywordRepository keywordRepo,
            TransactionCardSupport cardSupport) {
        this.categoryRepo = categoryRepo;
        this.txRepo = txRepo;
        this.fixedRepo = fixedRepo;
        this.keywordRepo = keywordRepo;
        this.cardSupport = cardSupport;
    }

    public CategoryListResponse listCategories(LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        return new CategoryListResponse(
                treeFor(ledger, CategoryType.EXPENSE),
                treeFor(ledger, CategoryType.INCOME),
                treeFor(ledger, CategoryType.SAVINGS),
                treeFor(ledger, CategoryType.INSURANCE));
    }

    @Transactional
    public CategoryDto create(LedgerBook book, CategoryCreateRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        String name = normalize(req.name());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("이름을 입력하세요.");
        }
        CategoryTier tier = req.tier() != null ? req.tier() : CategoryTier.MINOR;
        if (tier == CategoryTier.MAJOR) {
            if (req.parentId() != null) {
                throw new IllegalArgumentException("대분류에는 상위 분류를 지정할 수 없습니다.");
            }
            if (categoryRepo.existsByBookAndCategoryTypeAndNameAndTier(
                    ledger, req.categoryType(), name, CategoryTier.MAJOR)) {
                throw new IllegalArgumentException("이미 등록된 분류입니다: " + name);
            }
            CbCategory row = new CbCategory();
            row.setBook(ledger);
            row.setCategoryType(req.categoryType());
            row.setTier(CategoryTier.MAJOR);
            row.setName(name);
            row.setSortOrder(nextMajorOrder(ledger, req.categoryType()));
            row.setUserCreated(true);
            categoryRepo.save(row);
            return toDto(row);
        }

        Long parentId = req.parentId();
        if (parentId == null) {
            throw new IllegalArgumentException("소분류는 대분류를 선택하세요.");
        }
        CbCategory parent = requireMajor(ledger, parentId, req.categoryType());
        if (categoryRepo.existsByBookAndCategoryTypeAndNameAndTierAndParentId(
                ledger, req.categoryType(), name, CategoryTier.MINOR, parent.getId())) {
            throw new IllegalArgumentException("이미 등록된 분류입니다: " + name);
        }
        CbCategory row = new CbCategory();
        row.setBook(ledger);
        row.setCategoryType(req.categoryType());
        row.setTier(CategoryTier.MINOR);
        row.setParentId(parent.getId());
        row.setName(name);
        row.setSortOrder(nextMinorOrder(ledger, req.categoryType(), parent.getId()));
        row.setUserCreated(true);
        categoryRepo.save(row);
        return toDto(row);
    }

    @Transactional
    public CategoryDto update(Long id, LedgerBook book, CategoryUpdateRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        CbCategory row =
                categoryRepo
                        .findByIdAndBook(id, ledger)
                        .orElseThrow(() -> new IllegalArgumentException("분류 없음: " + id));
        String newName = normalize(req.name());
        if (newName.isEmpty()) {
            throw new IllegalArgumentException("이름을 입력하세요.");
        }
        String oldName = row.getName();
        boolean nameChanged = !oldName.equals(newName);
        if (nameChanged) {
            Long parentId = row.getParentId();
            var duplicate =
                    parentId != null && row.getTier() == CategoryTier.MINOR
                            ? categoryRepo.findByBookAndCategoryTypeAndNameAndTierAndParentId(
                                    ledger, row.getCategoryType(), newName, row.getTier(), parentId)
                            : categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
                                    ledger, row.getCategoryType(), newName, row.getTier());
            if (duplicate.isPresent() && !duplicate.get().getId().equals(row.getId())) {
                throw new IllegalArgumentException("이미 등록된 분류입니다: " + newName);
            }
        }
        if (row.getTier() == CategoryTier.MINOR && req.parentId() != null) {
            CbCategory parent = requireMajor(ledger, req.parentId(), row.getCategoryType());
            row.setParentId(parent.getId());
        }
        if (nameChanged && row.getTier() == CategoryTier.MINOR) {
            propagateRename(ledger, row.getCategoryType(), oldName, newName);
            row.setName(newName);
        } else if (nameChanged) {
            row.setName(newName);
        }
        categoryRepo.save(row);
        return toDto(row);
    }

    @Transactional
    public void reorder(LedgerBook book, CategoryReorderRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        for (CategoryReorderItem item : req.items()) {
            CbCategory row =
                    categoryRepo
                            .findByIdAndBook(item.id(), ledger)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("분류 없음: " + item.id()));
            row.setSortOrder(item.sortOrder());
            if (row.getTier() == CategoryTier.MINOR && item.parentId() != null) {
                requireMajor(ledger, item.parentId(), row.getCategoryType());
                row.setParentId(item.parentId());
            }
            categoryRepo.save(row);
        }
    }

    @Transactional
    public void delete(Long id, LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        CbCategory row =
                categoryRepo
                        .findByIdAndBook(id, ledger)
                        .orElseThrow(() -> new IllegalArgumentException("분류 없음: " + id));
        if (row.getTier() == CategoryTier.MAJOR) {
            long children =
                    categoryRepo.countByBookAndCategoryTypeAndParentIdAndTier(
                            ledger, row.getCategoryType(), row.getId(), CategoryTier.MINOR);
            if (children > 0) {
                throw new IllegalArgumentException("하위 소분류가 있어 삭제할 수 없습니다.");
            }
        }
        if (isCategoryInUse(ledger, row)) {
            throw new IllegalArgumentException("내역에 사용 중인 분류는 삭제할 수 없습니다.");
        }
        categoryRepo.delete(row);
    }

    public CategoryTransactionsResponse listTransactionsForCategory(Long id, LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        CbCategory row =
                categoryRepo
                        .findByIdAndBook(id, ledger)
                        .orElseThrow(() -> new IllegalArgumentException("분류 없음: " + id));
        if (row.getCategoryType() != CategoryType.INCOME
                && row.getCategoryType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("수입·지출 분류만 조회할 수 있습니다.");
        }

        List<CbTransaction> txs = loadTransactionsForCategory(ledger, row);
        Map<Long, String> cardNames = cardSupport.cardNameIndex(ledger);
        BigDecimal total = BigDecimal.ZERO;
        List<CategoryTransactionRowDto> items = new ArrayList<>();
        for (CbTransaction tx : txs) {
            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            total = total.add(amount);
            String cardName =
                    tx.getCardProductId() != null
                            ? cardNames.getOrDefault(tx.getCardProductId(), "")
                            : "";
            items.add(
                    new CategoryTransactionRowDto(
                            tx.getId(),
                            tx.getTxDate().toString(),
                            txTypeLabel(tx.getTxType()),
                            tx.getTitle() != null ? tx.getTitle() : "",
                            amount,
                            tx.getCategoryId(),
                            tx.getCardProductId(),
                            cardName,
                            tx.getRemarks() != null ? tx.getRemarks() : ""));
        }

        return new CategoryTransactionsResponse(
                row.getId(),
                row.getName(),
                row.getCategoryType(),
                row.getTier(),
                items.size(),
                total,
                items);
    }

    public CategoryTransactionTableResponse listTransactionTableForCategory(Long id, LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        CbCategory row =
                categoryRepo
                        .findByIdAndBook(id, ledger)
                        .orElseThrow(() -> new IllegalArgumentException("분류 없음: " + id));
        if (row.getCategoryType() != CategoryType.INCOME
                && row.getCategoryType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException("수입·지출 분류만 조회할 수 있습니다.");
        }

        List<CbTransaction> txs = loadTransactionsForCategory(ledger, row);
        List<Long> categoryIds = resolveCategoryIdsForTransactions(ledger, row);
        List<TransactionTableRowDto> rows =
                txs.stream().map(TransactionTableSupport::toRow).toList();
        String querySql = TransactionTableSupport.queryForCategory(ledger, categoryIds);

        return new CategoryTransactionTableResponse(
                TransactionTableSupport.TABLE_NAME,
                row.getId(),
                row.getName(),
                rows.size(),
                querySql,
                rows);
    }

    private List<CbTransaction> loadTransactionsForCategory(LedgerBook ledger, CbCategory row) {
        List<Long> categoryIds = resolveCategoryIdsForTransactions(ledger, row);
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return txRepo.findByBookAndCategoryIdInOrderByTxDateDescSortOrderAscIdDesc(
                ledger, categoryIds);
    }

    private List<Long> resolveCategoryIdsForTransactions(LedgerBook ledger, CbCategory row) {
        if (row.getTier() == CategoryTier.MINOR) {
            return allMinorIdsByName(ledger, row.getCategoryType(), row.getName());
        }
        return categoryRepo
                .findByBookAndCategoryTypeAndParentIdOrderBySortOrderAscNameAsc(
                        ledger, row.getCategoryType(), row.getId())
                .stream()
                .flatMap(
                        child ->
                                allMinorIdsByName(ledger, row.getCategoryType(), child.getName())
                                        .stream())
                .distinct()
                .toList();
    }

    /** 동명 소분류가 여러 ID로 존재할 때(마이그레이션·재구성 잔여) 모두 조회합니다. */
    private List<Long> allMinorIdsByName(LedgerBook ledger, CategoryType type, String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return categoryRepo
                .findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        ledger, type, CategoryTier.MINOR)
                .stream()
                .filter(c -> name.equals(c.getName()))
                .map(CbCategory::getId)
                .toList();
    }

    private static String txTypeLabel(TxType type) {
        return switch (type) {
            case EXPENSE -> "지출";
            case INCOME -> "수입";
            case SAVINGS -> "저축";
            default -> type.name();
        };
    }

    @Transactional
    public void savePreferences(LedgerBook book, CategoryPreferencesRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        for (CategoryPreferenceItem item : req.items()) {
            CbCategory row =
                    categoryRepo
                            .findByIdAndBook(item.id(), ledger)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("분류 없음: " + item.id()));
            if (item.enabled() != null) {
                row.setEnabled(item.enabled());
            }
            if (item.fixedExpense() != null) {
                row.setFixedExpense(item.fixedExpense());
            }
            categoryRepo.save(row);
        }
    }

    @Transactional
    public void ensureMinorCategory(LedgerBook book, CategoryType type, String fullName) {
        LedgerBook ledger = bookOrDefault(book);
        String name = normalize(fullName);
        if (name.isEmpty()) return;
        if (type == CategoryType.INCOME) {
            ensureCatalogMinor(ledger, CategoryType.INCOME, name, incomeMinorToMajor(ledger));
            return;
        }
        if (type == CategoryType.EXPENSE) {
            ensureCatalogMinor(ledger, CategoryType.EXPENSE, name, expenseMinorToMajor(ledger));
            return;
        }
        if (categoryRepo.existsByBookAndCategoryTypeAndNameAndTier(
                ledger, type, name, CategoryTier.MINOR)) {
            return;
        }
        String majorName = majorNameFrom(name);
        CbCategory major = findOrCreateMajor(ledger, type, majorName);
        createMinor(ledger, type, name, major.getId());
    }

    @Transactional
    public int ensureIncomeHierarchy(LedgerBook book) {
        return migrateIncomeHierarchy(bookOrDefault(book));
    }

    @Transactional
    public int ensureExpenseHierarchy(LedgerBook book) {
        return migrateExpenseHierarchy(bookOrDefault(book));
    }

    @Transactional
    public int migrateIncomeHierarchy(LedgerBook ledger) {
        LedgerBook book = bookOrDefault(ledger);
        int changed = 0;
        if (book == LedgerBook.PERSONAL) {
            changed += reparentIncomeGitaMajor(book);
        }
        Map<String, String> minorToMajor = incomeMinorToMajor(book);
        changed +=
                migrateHierarchy(
                        book, CategoryType.INCOME, incomeCatalogGroups(book), minorToMajor);
        return changed;
    }

    /**
     * 수입 대분류 '기타' 잔존 항목을 부수입 소분류로 옮깁니다.
     * 주수입 하위 '기타' 소분류는 그대로 둡니다.
     */
    private int reparentIncomeGitaMajor(LedgerBook ledger) {
        var gitaMajor =
                categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
                        ledger, CategoryType.INCOME, "기타", CategoryTier.MAJOR);
        if (gitaMajor.isEmpty()) {
            return 0;
        }
        CbCategory sideIncome =
                ensureCatalogMajor(
                        ledger,
                        CategoryType.INCOME,
                        "부수입",
                        majorSortOrder(
                                ledger,
                                CategoryType.INCOME,
                                "부수입",
                                incomeMinorToMajor(ledger)));

        var existingUnderSide =
                categoryRepo.findByBookAndCategoryTypeAndNameAndTierAndParentId(
                        ledger,
                        CategoryType.INCOME,
                        "기타",
                        CategoryTier.MINOR,
                        sideIncome.getId());
        if (existingUnderSide.isPresent()) {
            categoryRepo.delete(gitaMajor.get());
            return 1;
        }

        CbCategory majorRow = gitaMajor.get();
        majorRow.setTier(CategoryTier.MINOR);
        majorRow.setParentId(sideIncome.getId());
        if (majorRow.getSortOrder() == null) {
            majorRow.setSortOrder(1);
        }
        categoryRepo.save(majorRow);
        return 1;
    }

    @Transactional
    public int migrateExpenseHierarchy(LedgerBook ledger) {
        LedgerBook book = bookOrDefault(ledger);
        int changed = 0;
        if (book == LedgerBook.PERSONAL) {
            changed += removeDeprecatedExpenseMajor(book, "낚시", "취미");
        }
        Map<String, String> minorToMajor = expenseMinorToMajor(book);
        changed +=
                migrateHierarchy(
                        book, CategoryType.EXPENSE, expenseCatalogGroups(book), minorToMajor);
        return changed;
    }

    /** 폐기된 지출 대분류의 잔여 소분류를 옮기고, 빈 대분류를 제거합니다. */
    private int removeDeprecatedExpenseMajor(
            LedgerBook ledger, String deprecatedMajorName, String targetMajorName) {
        CbCategory deprecated = findMajor(ledger, CategoryType.EXPENSE, deprecatedMajorName);
        if (deprecated == null) {
            return 0;
        }
        CbCategory target =
                ensureCatalogMajor(
                        ledger,
                        CategoryType.EXPENSE,
                        targetMajorName,
                        majorSortOrder(
                                ledger,
                                CategoryType.EXPENSE,
                                targetMajorName,
                                expenseMinorToMajor(ledger)));

        int changed = 0;
        var children =
                categoryRepo.findByBookAndCategoryTypeAndParentIdOrderBySortOrderAscNameAsc(
                        ledger, CategoryType.EXPENSE, deprecated.getId());
        for (CbCategory child : children) {
            var underTarget =
                    categoryRepo.findByBookAndCategoryTypeAndNameAndTierAndParentId(
                            ledger,
                            CategoryType.EXPENSE,
                            child.getName(),
                            CategoryTier.MINOR,
                            target.getId());
            if (underTarget.isPresent()) {
                if (!isCategoryInUse(ledger, child)) {
                    categoryRepo.delete(child);
                    changed++;
                }
            } else {
                child.setParentId(target.getId());
                categoryRepo.save(child);
                changed++;
            }
        }

        long remaining =
                categoryRepo.countByBookAndCategoryTypeAndParentIdAndTier(
                        ledger, CategoryType.EXPENSE, deprecated.getId(), CategoryTier.MINOR);
        if (remaining == 0) {
            categoryRepo.delete(deprecated);
            changed++;
        }
        return changed;
    }

    private record CatalogGroup(String major, List<String> minors) {}

    private static List<CatalogGroup> incomeCatalogGroups(LedgerBook book) {
        if (book == LedgerBook.HOUSEHOLD) {
            return HouseholdIncomeCategoryCatalog.GROUPS.stream()
                    .map(g -> new CatalogGroup(g.major(), g.minors()))
                    .toList();
        }
        return MonetaIncomeCategoryCatalog.GROUPS.stream()
                .map(g -> new CatalogGroup(g.major(), g.minors()))
                .toList();
    }

    private static Map<String, String> incomeMinorToMajor(LedgerBook book) {
        return book == LedgerBook.HOUSEHOLD
                ? HouseholdIncomeCategoryCatalog.minorToMajor()
                : MonetaIncomeCategoryCatalog.minorToMajor();
    }

    private static List<CatalogGroup> expenseCatalogGroups(LedgerBook book) {
        if (book == LedgerBook.HOUSEHOLD) {
            return HouseholdExpenseCategoryCatalog.GROUPS.stream()
                    .map(g -> new CatalogGroup(g.major(), g.minors()))
                    .toList();
        }
        return MonetaExpenseCategoryCatalog.GROUPS.stream()
                .map(g -> new CatalogGroup(g.major(), g.minors()))
                .toList();
    }

    private static Map<String, String> expenseMinorToMajor(LedgerBook book) {
        return book == LedgerBook.HOUSEHOLD
                ? HouseholdExpenseCategoryCatalog.minorToMajor()
                : MonetaExpenseCategoryCatalog.minorToMajor();
    }

    private int migrateHierarchy(
            LedgerBook ledger,
            CategoryType type,
            List<CatalogGroup> groups,
            Map<String, String> minorToMajor) {
        int changed = dissolveLegacyMajors(ledger, type, groups);
        int majorOrder = 0;
        Set<Long> assignedMinorIds = new HashSet<>();

        for (CatalogGroup group : groups) {
            CbCategory major = ensureCatalogMajor(ledger, type, group.major(), majorOrder++);
            int minorOrder = 0;
            for (String minorName : group.minors()) {
                changed +=
                        assignCatalogMinor(
                                ledger,
                                type,
                                major,
                                minorName,
                                minorOrder++,
                                assignedMinorIds,
                                minorToMajor);
            }
        }

        changed += cleanupCatalogMajors(ledger, type, groups, assignedMinorIds, minorToMajor);
        changed += removeStaleCatalogMinors(ledger, type, minorToMajor);
        return changed;
    }

    /** 카탈로그에서 빠진 기본 소분류 중 미사용 항목을 제거합니다. */
    private int removeStaleCatalogMinors(
            LedgerBook ledger, CategoryType type, Map<String, String> minorToMajor) {
        if (type != CategoryType.INCOME
                && !(type == CategoryType.EXPENSE && ledger == LedgerBook.PERSONAL)) {
            return 0;
        }
        Set<String> catalogMinors = minorToMajor.keySet();
        int removed = 0;
        var minors =
                categoryRepo.findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        ledger, type, CategoryTier.MINOR);
        for (CbCategory row : minors) {
            if (Boolean.TRUE.equals(row.getUserCreated())) {
                continue;
            }
            if (catalogMinors.contains(row.getName())) {
                continue;
            }
            if (isCategoryInUse(ledger, row)) {
                continue;
            }
            categoryRepo.delete(row);
            removed++;
        }
        return removed;
    }

    private int dissolveLegacyMajors(
            LedgerBook ledger, CategoryType type, List<CatalogGroup> groups) {
        Set<String> validMajors =
                groups.stream().map(CatalogGroup::major).collect(java.util.stream.Collectors.toSet());
        int changed = 0;
        var majors =
                categoryRepo.findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        ledger, type, CategoryTier.MAJOR);
        for (CbCategory major : majors) {
            if (validMajors.contains(major.getName())) {
                continue;
            }
            var children =
                    categoryRepo.findByBookAndCategoryTypeAndParentIdOrderBySortOrderAscNameAsc(
                            ledger, type, major.getId());
            for (CbCategory child : children) {
                child.setParentId(null);
                categoryRepo.save(child);
                changed++;
            }
            categoryRepo.delete(major);
            changed++;
        }
        return changed;
    }

    @Transactional
    public int migrateFlatToHierarchy(LedgerBook book) {
        LedgerBook ledger = bookOrDefault(book);
        int migrated = 0;
        for (CategoryType type : CategoryType.values()) {
            if (type == CategoryType.INCOME) {
                continue;
            }
            if (type == CategoryType.EXPENSE && ledger == LedgerBook.PERSONAL) {
                continue;
            }
            List<CbCategory> legacy =
                    categoryRepo.findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(ledger, type);
        List<CbCategory> pending = new ArrayList<>();
            for (CbCategory row : legacy) {
                if (row.getTier() == CategoryTier.MAJOR) {
                    continue;
                }
                if (row.getParentId() != null) {
                    continue;
                }
                pending.add(row);
            }
            if (pending.isEmpty()) {
                continue;
            }
            Map<String, CbCategory> majors = new HashMap<>();
            int[] majorOrder = {nextMajorOrder(ledger, type)};
            for (CbCategory row : pending) {
                String majorName = majorNameFrom(row.getName());
                majors.computeIfAbsent(
                        majorName, n -> resolveOrCreateMajor(ledger, type, n, pending, majorOrder));
            }
            for (CbCategory row : pending) {
                if (row.getTier() == CategoryTier.MAJOR) {
                    continue;
                }
                String majorName = majorNameFrom(row.getName());
                CbCategory major = majors.get(majorName);
                if (major.getId().equals(row.getId())) {
                    continue;
                }
                row.setTier(CategoryTier.MINOR);
                row.setParentId(major.getId());
                if (row.getSortOrder() == null) {
                    row.setSortOrder(0);
                }
                categoryRepo.save(row);
                migrated++;
            }
        }
        return migrated;
    }

    private void ensureCatalogMinor(
            LedgerBook ledger,
            CategoryType type,
            String minorName,
            Map<String, String> minorToMajor) {
        String majorName = minorToMajor.getOrDefault(minorName, DEFAULT_MAJOR);
        CbCategory major =
                ensureCatalogMajor(
                        ledger, type, majorName, majorSortOrder(ledger, type, majorName, minorToMajor));
        if (categoryRepo.existsByBookAndCategoryTypeAndNameAndTierAndParentId(
                ledger, type, minorName, CategoryTier.MINOR, major.getId())) {
            return;
        }
        createMinor(ledger, type, minorName, major.getId());
    }

    private CbCategory ensureCatalogMajor(
            LedgerBook ledger, CategoryType type, String majorName, int sortOrder) {
        CbCategory existing = findMajor(ledger, type, majorName);
        if (existing != null) {
            if (existing.getSortOrder() == null) {
                existing.setSortOrder(sortOrder);
                categoryRepo.save(existing);
            }
            return existing;
        }
        CbCategory row = new CbCategory();
        row.setBook(ledger);
        row.setCategoryType(type);
        row.setTier(CategoryTier.MAJOR);
        row.setName(majorName);
        row.setSortOrder(sortOrder);
        categoryRepo.save(row);
        return row;
    }

    private int assignCatalogMinor(
            LedgerBook ledger,
            CategoryType type,
            CbCategory major,
            String minorName,
            int sortOrder,
            Set<Long> assignedMinorIds,
            Map<String, String> minorToMajor) {
        var existingMinor =
                categoryRepo.findByBookAndCategoryTypeAndNameAndTierAndParentId(
                        ledger, type, minorName, CategoryTier.MINOR, major.getId());
        if (existingMinor.isPresent()) {
            CbCategory minor = existingMinor.get();
            boolean changed = false;
            if (!major.getId().equals(minor.getParentId())) {
                minor.setParentId(major.getId());
                changed = true;
            }
            if (minor.getSortOrder() == null) {
                minor.setSortOrder(sortOrder);
                changed = true;
            }
            if (changed) {
                categoryRepo.save(minor);
            }
            assignedMinorIds.add(minor.getId());
            return changed ? 1 : 0;
        }

        if (major.getName().equals(minorName)
                && categoryRepo
                        .findByBookAndCategoryTypeAndNameAndTier(
                                ledger, type, minorName, CategoryTier.MAJOR)
                        .isPresent()) {
            createMinor(ledger, type, minorName, major.getId());
            categoryRepo
                    .findByBookAndCategoryTypeAndNameAndTierAndParentId(
                            ledger, type, minorName, CategoryTier.MINOR, major.getId())
                    .ifPresent(m -> assignedMinorIds.add(m.getId()));
            return 1;
        }

        var sameNameMajor =
                categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
                        ledger, type, minorName, CategoryTier.MAJOR);
        if (sameNameMajor.isPresent() && !sameNameMajor.get().getId().equals(major.getId())) {
            categoryRepo.delete(sameNameMajor.get());
        }

        var orphan =
                categoryRepo
                        .findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(ledger, type)
                        .stream()
                        .filter(
                                c ->
                                        minorName.equals(c.getName())
                                                && c.getTier() != CategoryTier.MAJOR
                                                && c.getParentId() == null)
                        .findFirst();
        if (orphan.isPresent()) {
            CbCategory minor = orphan.get();
            minor.setTier(CategoryTier.MINOR);
            minor.setParentId(major.getId());
            minor.setSortOrder(sortOrder);
            categoryRepo.save(minor);
            assignedMinorIds.add(minor.getId());
            return 1;
        }

        createMinor(ledger, type, minorName, major.getId());
        categoryRepo
                .findByBookAndCategoryTypeAndNameAndTierAndParentId(
                        ledger, type, minorName, CategoryTier.MINOR, major.getId())
                .ifPresent(m -> assignedMinorIds.add(m.getId()));
        return 1;
    }

    private int cleanupCatalogMajors(
            LedgerBook ledger,
            CategoryType type,
            List<CatalogGroup> groups,
            Set<Long> assignedMinorIds,
            Map<String, String> minorToMajor) {
        int removed = 0;
        Set<String> validMajors =
                groups.stream().map(CatalogGroup::major).collect(java.util.stream.Collectors.toSet());

        var majors =
                categoryRepo.findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        ledger, type, CategoryTier.MAJOR);
        for (CbCategory major : majors) {
            if (!validMajors.contains(major.getName())) {
                long children =
                        categoryRepo.countByBookAndCategoryTypeAndParentIdAndTier(
                                ledger, type, major.getId(), CategoryTier.MINOR);
                if (children == 0) {
                    categoryRepo.delete(major);
                    removed++;
                }
            }
        }

        var all = categoryRepo.findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(ledger, type);
        for (CbCategory row : all) {
            if (row.getTier() == CategoryTier.MAJOR) {
                continue;
            }
            if (row.getParentId() == null && !assignedMinorIds.contains(row.getId())) {
                String majorName = minorToMajor.getOrDefault(row.getName(), DEFAULT_MAJOR);
                CbCategory major =
                        ensureCatalogMajor(
                                ledger, type, majorName, majorSortOrder(ledger, type, majorName, minorToMajor));
                row.setTier(CategoryTier.MINOR);
                row.setParentId(major.getId());
                categoryRepo.save(row);
                removed++;
            }
        }
        return removed;
    }

    private int majorSortOrder(
            LedgerBook ledger,
            CategoryType type,
            String majorName,
            Map<String, String> minorToMajor) {
        List<CatalogGroup> groups =
                type == CategoryType.INCOME
                        ? incomeCatalogGroups(ledger)
                        : expenseCatalogGroups(ledger);
        int idx = 0;
        for (CatalogGroup group : groups) {
            if (group.major().equals(majorName)) {
                return idx;
            }
            idx++;
        }
        return idx;
    }

    private void createMinor(LedgerBook ledger, CategoryType type, String name, Long parentId) {
        CbCategory minor = new CbCategory();
        minor.setBook(ledger);
        minor.setCategoryType(type);
        minor.setTier(CategoryTier.MINOR);
        minor.setParentId(parentId);
        minor.setName(name);
        minor.setSortOrder(nextMinorOrder(ledger, type, parentId));
        categoryRepo.save(minor);
    }

    private CbCategory resolveOrCreateMajor(
            LedgerBook ledger,
            CategoryType type,
            String majorName,
            List<CbCategory> pending,
            int[] majorOrder) {
        CbCategory existing = findMajor(ledger, type, majorName);
        if (existing != null) {
            return existing;
        }
        for (CbCategory row : pending) {
            if (majorName.equals(row.getName()) && row.getTier() != CategoryTier.MAJOR) {
                row.setTier(CategoryTier.MAJOR);
                row.setParentId(null);
                row.setSortOrder(majorOrder[0]++);
                categoryRepo.save(row);
                return row;
            }
        }
        CbCategory created = new CbCategory();
        created.setBook(ledger);
        created.setCategoryType(type);
        created.setTier(CategoryTier.MAJOR);
        created.setName(majorName);
        created.setSortOrder(majorOrder[0]++);
        categoryRepo.save(created);
        return created;
    }

    private List<CategoryGroupDto> treeFor(LedgerBook book, CategoryType type) {
        Set<String> usedMinorNames = usedMinorNamesFor(book, type);
        List<CbCategory> majors =
                categoryRepo.findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        book, type, CategoryTier.MAJOR);
        return majors.stream()
                .map(
                        major -> {
                            List<CategoryDto> children =
                                    categoryRepo
                                            .findByBookAndCategoryTypeAndParentIdOrderBySortOrderAscNameAsc(
                                                    book, type, major.getId())
                                            .stream()
                                            .map(
                                                    child ->
                                                            toDto(
                                                                    child,
                                                                    usedMinorNames.contains(
                                                                            child.getName())))
                                            .toList();
                            boolean majorInUse =
                                    children.stream().anyMatch(CategoryDto::inUse);
                            return new CategoryGroupDto(
                                    major.getId(),
                                    major.getName(),
                                    major.getSortOrder() != null ? major.getSortOrder() : 0,
                                    Boolean.TRUE.equals(major.getEnabled()),
                                    Boolean.TRUE.equals(major.getUserCreated()),
                                    Boolean.TRUE.equals(major.getFixedExpense()),
                                    majorInUse,
                                    children);
                        })
                .toList();
    }

    private Set<String> usedMinorNamesFor(LedgerBook book, CategoryType type) {
        TxType txType =
                switch (type) {
                    case EXPENSE -> TxType.EXPENSE;
                    case INCOME -> TxType.INCOME;
                    default -> null;
                };
        if (txType == null) {
            return Set.of();
        }
        return txRepo.findDistinctCategoryIds(book, txType).stream()
                .map(id -> categoryRepo.findByIdAndBook(id, book).map(CbCategory::getName).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));
    }

    private boolean isCategoryInUse(LedgerBook book, CbCategory row) {
        TxType txType =
                switch (row.getCategoryType()) {
                    case EXPENSE -> TxType.EXPENSE;
                    case INCOME -> TxType.INCOME;
                    default -> null;
                };
        if (txType == null || row.getTier() != CategoryTier.MINOR) {
            return false;
        }
        for (Long categoryId : allMinorIdsByName(book, row.getCategoryType(), row.getName())) {
            if (txRepo.existsByBookAndTxTypeAndCategoryId(book, txType, categoryId)) {
                return true;
            }
        }
        return false;
    }

    private CbCategory requireMajor(LedgerBook book, Long parentId, CategoryType type) {
        CbCategory parent =
                categoryRepo
                        .findByIdAndBook(parentId, book)
                        .orElseThrow(() -> new IllegalArgumentException("대분류 없음: " + parentId));
        if (parent.getTier() != CategoryTier.MAJOR || parent.getCategoryType() != type) {
            throw new IllegalArgumentException("유효하지 않은 대분류입니다.");
        }
        return parent;
    }

    private CbCategory findOrCreateMajor(LedgerBook book, CategoryType type, String majorName) {
        CbCategory existing = findMajor(book, type, majorName);
        if (existing != null) {
            return existing;
        }
        for (CbCategory row :
                categoryRepo.findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(book, type)) {
            if (majorName.equals(row.getName())
                    && row.getTier() != CategoryTier.MAJOR
                    && row.getParentId() == null) {
                row.setTier(CategoryTier.MAJOR);
                row.setParentId(null);
                if (row.getSortOrder() == null) {
                    row.setSortOrder(nextMajorOrder(book, type));
                }
                categoryRepo.save(row);
                return row;
            }
        }
        CbCategory row = new CbCategory();
        row.setBook(book);
        row.setCategoryType(type);
        row.setTier(CategoryTier.MAJOR);
        row.setName(majorName);
        row.setSortOrder(nextMajorOrder(book, type));
        categoryRepo.save(row);
        return row;
    }

    private CbCategory findMajor(LedgerBook book, CategoryType type, String majorName) {
        return categoryRepo
                .findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        book, type, CategoryTier.MAJOR)
                .stream()
                .filter(c -> majorName.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }

    private int nextMajorOrder(LedgerBook book, CategoryType type) {
        return categoryRepo
                .findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        book, type, CategoryTier.MAJOR)
                .size();
    }

    private int nextMinorOrder(LedgerBook book, CategoryType type, Long parentId) {
        return categoryRepo
                .findByBookAndCategoryTypeAndParentIdOrderBySortOrderAscNameAsc(
                        book, type, parentId)
                .size();
    }

    private static String majorNameFrom(String fullName) {
        int idx = fullName.indexOf(':');
        if (idx > 0) {
            return fullName.substring(0, idx).trim();
        }
        return DEFAULT_MAJOR;
    }

    private void propagateRename(
            LedgerBook book, CategoryType type, String oldName, String newName) {
        switch (type) {
            case EXPENSE -> {
                fixedRepo.renameCategory(book, TxType.EXPENSE, oldName, newName);
                keywordRepo.renameCategoryName(book, TxType.EXPENSE, oldName, newName);
            }
            case INCOME -> {
                fixedRepo.renameCategory(book, TxType.INCOME, oldName, newName);
                keywordRepo.renameCategoryName(book, TxType.INCOME, oldName, newName);
            }
            case SAVINGS -> {
                fixedRepo.renameTitle(book, TxType.SAVINGS, oldName, newName);
            }
            case INSURANCE -> {
                fixedRepo.renameTitle(book, TxType.SAVINGS, oldName, newName);
            }
        }
    }

    private static LedgerBook bookOrDefault(LedgerBook book) {
        return book != null ? book : LedgerBook.PERSONAL;
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }

    private static CategoryDto toDto(CbCategory row, boolean inUse) {
        return new CategoryDto(
                row.getId(),
                row.getCategoryType(),
                row.getName(),
                row.getTier(),
                row.getParentId(),
                row.getSortOrder() != null ? row.getSortOrder() : 0,
                Boolean.TRUE.equals(row.getEnabled()),
                Boolean.TRUE.equals(row.getUserCreated()),
                Boolean.TRUE.equals(row.getFixedExpense()),
                inUse);
    }

    private static CategoryDto toDto(CbCategory row) {
        return toDto(row, false);
    }
}
