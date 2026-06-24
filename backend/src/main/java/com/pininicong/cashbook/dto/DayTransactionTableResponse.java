package com.pininicong.cashbook.dto;

import java.util.List;

public record DayTransactionTableResponse(
        String tableName,
        String txDate,
        String book,
        String bookLabel,
        int count,
        String querySql,
        List<TransactionTableRowDto> rows) {}
