package com.pininicong.cashbook.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(@NotBlank String name, Long parentId) {}
