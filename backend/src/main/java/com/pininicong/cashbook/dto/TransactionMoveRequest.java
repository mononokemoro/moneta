package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.LedgerBook;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TransactionMoveRequest(
        @NotNull LocalDate targetDate,
        @NotEmpty List<Long> ids,
        LedgerBook book) {}
