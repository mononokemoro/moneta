package com.pininicong.cashbook.dto;

import java.math.BigDecimal;
import java.util.List;

public record DayViewDto(
        String date,
        String yearMonth,
        BudgetDto budget,
        BigDecimal cashBalance,
        List<TransactionRowDto> expenses,
        List<TransactionRowDto> incomes,
        List<SavingsRowDto> savings,
        String scheduleNote,
        String dayMemo,
        PaymentSummaryDto paymentSummary) {}
