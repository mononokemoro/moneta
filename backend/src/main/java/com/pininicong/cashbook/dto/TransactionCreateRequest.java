package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateRequest(
        @NotNull LocalDate txDate,
        @NotNull TxType txType,
        String title,
        @NotNull BigDecimal amount,
        String category,
        String cardName,
        String remarks,
        BigDecimal accumulatedAmount,
        LedgerBook book) {}
