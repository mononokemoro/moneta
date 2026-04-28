package com.pininicong.cashbook.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetUpdateRequest(@NotNull BigDecimal totalBudget) {}
