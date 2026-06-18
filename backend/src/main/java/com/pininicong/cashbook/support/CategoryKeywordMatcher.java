package com.pininicong.cashbook.support;

import com.pininicong.cashbook.domain.CbCategoryKeyword;
import java.util.Comparator;
import java.util.List;

public final class CategoryKeywordMatcher {

    private CategoryKeywordMatcher() {}

    public static String matchCategory(String title, List<CbCategoryKeyword> rules) {
        if (title == null || title.isBlank() || rules == null || rules.isEmpty()) {
            return null;
        }
        return rules.stream()
                .filter(r -> r.getKeyword() != null && !r.getKeyword().isBlank())
                .filter(r -> title.contains(r.getKeyword()))
                .max(
                        Comparator.comparingInt((CbCategoryKeyword r) -> r.getKeyword().length())
                                .thenComparing(r -> r.getSortOrder() != null ? r.getSortOrder() : 0))
                .map(CbCategoryKeyword::getCategoryName)
                .orElse(null);
    }
}
