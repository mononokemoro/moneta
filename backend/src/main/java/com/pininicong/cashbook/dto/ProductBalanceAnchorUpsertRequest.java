package com.pininicong.cashbook.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductBalanceAnchorUpsertRequest(
        Long productId,
        String title,
        @NotNull LocalDate anchorDate,
        @NotNull BigDecimal balance,
        String remarks) {}
