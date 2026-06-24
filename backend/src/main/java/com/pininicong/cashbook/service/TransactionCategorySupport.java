package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.repo.CbCategoryRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 거래 ↔ 분류 연결 (category_id 기준, 표시명 조회). */
@Service
public class TransactionCategorySupport {

    public record ResolvedCategory(Long id, String name) {}

    private final CbCategoryRepository categoryRepo;

    public TransactionCategorySupport(CbCategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    public static CategoryType categoryTypeFor(TxType txType) {
        return switch (txType) {
            case INCOME -> CategoryType.INCOME;
            case EXPENSE -> CategoryType.EXPENSE;
            case SAVINGS -> CategoryType.SAVINGS;
            default -> CategoryType.EXPENSE;
        };
    }

    /** id 우선, 없으면 name으로 소분류를 찾습니다. */
    public Optional<ResolvedCategory> resolveMinor(
            LedgerBook book, CategoryType type, Long categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryRepo
                    .findByIdAndBook(categoryId, book)
                    .filter(
                            c ->
                                    c.getTier() == CategoryTier.MINOR
                                            && c.getCategoryType() == type)
                    .map(c -> new ResolvedCategory(c.getId(), c.getName()));
        }
        String name = normalize(categoryName);
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return categoryRepo
                .findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        book, type, CategoryTier.MINOR)
                .stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(c -> new ResolvedCategory(c.getId(), c.getName()));
    }

    public String name(LedgerBook book, Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        return categoryRepo
                .findByIdAndBook(categoryId, book)
                .map(CbCategory::getName)
                .orElse("");
    }

    public Map<Long, String> minorNameIndex(LedgerBook book, CategoryType type) {
        Map<Long, String> map = new HashMap<>();
        for (CbCategory row :
                categoryRepo.findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        book, type, CategoryTier.MINOR)) {
            map.put(row.getId(), row.getName());
        }
        return map;
    }

    public Optional<Long> findMinorIdByName(LedgerBook book, CategoryType type, String name) {
        return resolveMinor(book, type, null, name).map(ResolvedCategory::id);
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }
}
