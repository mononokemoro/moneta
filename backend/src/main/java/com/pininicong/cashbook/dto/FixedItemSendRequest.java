package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.LedgerBook;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FixedItemSendRequest(
        @NotNull LocalDate txDate,
        @NotEmpty @Valid List<FixedItemSendEntry> entries,
        LedgerBook book) {

    public record FixedItemSendEntry(@NotNull Long fixedItemId, @NotNull BigDecimal amount) {}
}
