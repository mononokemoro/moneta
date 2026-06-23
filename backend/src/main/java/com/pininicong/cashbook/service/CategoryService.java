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
import com.pininicong.cashbook.imports.MonetaIncomeCategoryCatalog;
import com.pininicong.cashbook.imports.MonetaExpenseCategoryCatalog;
import com.pininicong.cashbook.repo.CbCategoryKeywordRepository;
import com.pininicong.cashbook.repo.CbCategoryRepository;
import com.pininicong.cashbook.repo.CbFixedItemRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
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

    public CategoryService(
            CbCategoryRepository categoryRepo,
            CbTransactionRepository txRepo,
            CbFixedItemRepository fixedRepo,
            CbCategoryKeywordRepository keywordRepo) {
        this.categoryRepo = categoryRepo;
        this.txRepo = txRepo;
        this.fixedRepo = fixedRepo;
        this.keywordRepo = keywordRepo;
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
        if (categoryRepo.existsByBookAndCategoryTypeAndNameAndTier(
                ledger, req.categoryType(), name, CategoryTier.MINOR)) {
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
            var duplicate =
                    categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
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
        if (!Boolean.TRUE.equals(row.getUserCreated())) {
            throw new IllegalArgumentException("기본항목은 삭제할 수 없습니다.");
        }
        categoryRepo.delete(row);
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
            ensureCatalogMinor(
                    ledger, CategoryType.INCOME, name, MonetaIncomeCategoryCatalog.minorToMajor());
            return;
        }
        if (type == CategoryType.EXPENSE && ledger == LedgerBook.PERSONAL) {
            ensureCatalogMinor(
                    ledger, CategoryType.EXPENSE, name, MonetaExpenseCategoryCatalog.minorToMajor());
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
        return migrateHierarchy(
                ledger,
                CategoryType.INCOME,
                incomeCatalogGroups(),
                MonetaIncomeCategoryCatalog.minorToMajor());
    }

    @Transactional
    public int migrateExpenseHierarchy(LedgerBook ledger) {
        return migrateHierarchy(
                ledger,
                CategoryType.EXPENSE,
                expenseCatalogGroups(),
                MonetaExpenseCategoryCatalog.minorToMajor());
    }

    private record CatalogGroup(String major, List<String> minors) {}

    private static List<CatalogGroup> incomeCatalogGroups() {
        return MonetaIncomeCategoryCatalog.GROUPS.stream()
                .map(g -> new CatalogGroup(g.major(), g.minors()))
                .toList();
    }

    private static List<CatalogGroup> expenseCatalogGroups() {
        return MonetaExpenseCategoryCatalog.GROUPS.stream()
                .map(g -> new CatalogGroup(g.major(), g.minors()))
                .toList();
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
        return changed;
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
        if (categoryRepo.existsByBookAndCategoryTypeAndNameAndTier(
                ledger, type, minorName, CategoryTier.MINOR)) {
            return;
        }
        String majorName = minorToMajor.getOrDefault(minorName, DEFAULT_MAJOR);
        CbCategory major =
                ensureCatalogMajor(
                        ledger, type, majorName, majorSortOrder(type, majorName, minorToMajor));
        createMinor(ledger, type, minorName, major.getId());
    }

    private CbCategory ensureCatalogMajor(
            LedgerBook ledger, CategoryType type, String majorName, int sortOrder) {
        CbCategory existing = findMajor(ledger, type, majorName);
        if (existing != null) {
            if (existing.getSortOrder() == null || existing.getSortOrder() != sortOrder) {
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
        String canonicalMajor = minorToMajor.get(minorName);
        if (canonicalMajor != null && !canonicalMajor.equals(major.getName())) {
            return 0;
        }

        var existingMinor =
                categoryRepo.findByBookAndCategoryTypeAndNameAndTier(
                        ledger, type, minorName, CategoryTier.MINOR);
        if (existingMinor.isPresent()) {
            CbCategory minor = existingMinor.get();
            boolean changed = false;
            if (!major.getId().equals(minor.getParentId())) {
                minor.setParentId(major.getId());
                changed = true;
            }
            if (minor.getSortOrder() == null || minor.getSortOrder() != sortOrder) {
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
                    .findByBookAndCategoryTypeAndNameAndTier(
                            ledger, type, minorName, CategoryTier.MINOR)
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
                .findByBookAndCategoryTypeAndNameAndTier(
                        ledger, type, minorName, CategoryTier.MINOR)
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
                                ledger, type, majorName, majorSortOrder(type, majorName, minorToMajor));
                row.setTier(CategoryTier.MINOR);
                row.setParentId(major.getId());
                categoryRepo.save(row);
                removed++;
            }
        }
        return removed;
    }

    private int majorSortOrder(
            CategoryType type, String majorName, Map<String, String> minorToMajor) {
        List<CatalogGroup> groups =
                type == CategoryType.INCOME ? incomeCatalogGroups() : expenseCatalogGroups();
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
        return new HashSet<>(txRepo.findDistinctCategoryNames(book, txType));
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
        return txRepo.existsByBookAndTxTypeAndCategory(book, txType, row.getName());
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
                txRepo.renameCategory(book, TxType.EXPENSE, oldName, newName);
                fixedRepo.renameCategory(book, TxType.EXPENSE, oldName, newName);
                keywordRepo.renameCategoryName(book, TxType.EXPENSE, oldName, newName);
            }
            case INCOME -> {
                txRepo.renameCategory(book, TxType.INCOME, oldName, newName);
                fixedRepo.renameCategory(book, TxType.INCOME, oldName, newName);
                keywordRepo.renameCategoryName(book, TxType.INCOME, oldName, newName);
            }
            case SAVINGS -> {
                txRepo.renameTitle(book, TxType.SAVINGS, oldName, newName, null);
                fixedRepo.renameTitle(book, TxType.SAVINGS, oldName, newName);
            }
            case INSURANCE -> {
                txRepo.renameTitle(book, TxType.SAVINGS, oldName, newName, "보험");
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
