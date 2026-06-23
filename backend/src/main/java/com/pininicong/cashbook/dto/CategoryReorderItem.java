package com.pininicong.cashbook.dto;

import jakarta.validation.constraints.NotNull;

public record CategoryReorderItem(@NotNull Long id, int sortOrder, Long parentId) {}
