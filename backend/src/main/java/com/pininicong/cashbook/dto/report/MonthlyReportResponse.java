package com.pininicong.cashbook.dto.report;

import java.util.List;

public record MonthlyReportResponse(
        int year,
        String period,
        String periodLabel,
        List<MonthColumnDto> columns,
        List<ReportRowDto> rows,
        List<ChartPointDto> expenseTrend) {}
