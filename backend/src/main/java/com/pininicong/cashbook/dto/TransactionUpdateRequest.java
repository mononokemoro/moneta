package com.pininicong.cashbook.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransactionUpdateRequest(
        String title,
        @NotNull BigDecimal amount,
        String category,
        String cardName,
        String remarks,
        BigDecimal accumulatedAmount) {}
