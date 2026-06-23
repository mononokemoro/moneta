package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryCreateRequest(
        @NotNull CategoryType categoryType,
        @NotNull CategoryTier tier,
        @NotBlank String name,
        Long parentId) {}
