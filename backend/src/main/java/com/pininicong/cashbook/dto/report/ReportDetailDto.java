package com.pininicong.cashbook.dto.report;

import java.util.List;

public record ReportDetailDto(
        String date,
        String txType,
        String title,
        String category,
        String cardName,
        long amount,
        String remarks) {}
