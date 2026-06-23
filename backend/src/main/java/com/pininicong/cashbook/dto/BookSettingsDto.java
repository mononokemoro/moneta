package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.ItemFieldMode;
import jakarta.validation.constraints.NotNull;

public record BookSettingsDto(
        ItemFieldMode expenseFieldMode,
        ItemFieldMode savingsInsuranceFieldMode,
        ItemFieldMode loanFieldMode,
        ItemFieldMode incomeFieldMode) {

    public record BookSettingsUpdateRequest(
            @NotNull ItemFieldMode expenseFieldMode,
            @NotNull ItemFieldMode savingsInsuranceFieldMode,
            @NotNull ItemFieldMode loanFieldMode,
            @NotNull ItemFieldMode incomeFieldMode) {}
}
