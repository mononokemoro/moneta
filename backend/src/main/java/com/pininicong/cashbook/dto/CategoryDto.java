package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import java.util.List;

public record CategoryDto(
        Long id,
        CategoryType categoryType,
        String name,
        CategoryTier tier,
        Long parentId,
        int sortOrder,
        boolean enabled,
        boolean userCreated,
        boolean fixedExpense,
        boolean inUse) {

    public record CategoryGroupDto(
            Long id,
            String name,
            int sortOrder,
            boolean enabled,
            boolean userCreated,
            boolean fixedExpense,
            boolean inUse,
            List<CategoryDto> children) {}

    public record CategoryListResponse(
            List<CategoryGroupDto> expense,
            List<CategoryGroupDto> income,
            List<CategoryGroupDto> savings,
            List<CategoryGroupDto> insurance) {}
}
