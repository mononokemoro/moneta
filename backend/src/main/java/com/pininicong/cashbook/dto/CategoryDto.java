package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import java.util.List;

public record CategoryDto(CategoryType categoryType, String name) {

    public record CategoryListResponse(List<CategoryDto> expense, List<CategoryDto> income, List<CategoryDto> savings, List<CategoryDto> insurance) {}
}
