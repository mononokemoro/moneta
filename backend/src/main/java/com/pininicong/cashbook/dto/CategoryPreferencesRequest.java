package com.pininicong.cashbook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CategoryPreferencesRequest(@NotEmpty @Valid List<CategoryPreferenceItem> items) {

    public record CategoryPreferenceItem(
            @NotNull Long id, Boolean enabled, Boolean fixedExpense) {}
}
