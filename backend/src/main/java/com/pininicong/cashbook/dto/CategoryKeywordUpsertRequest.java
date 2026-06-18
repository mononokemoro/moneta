package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryKeywordUpsertRequest(
        @NotNull TxType txType, @NotBlank String keyword, @NotBlank String categoryName) {}
