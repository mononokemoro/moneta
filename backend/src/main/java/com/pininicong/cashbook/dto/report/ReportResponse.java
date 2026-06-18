package com.pininicong.cashbook.dto.report;

import java.util.List;

public record ReportResponse(
        int year,
        String period,
        String periodLabel,
        String category,
        String subView,
        String granularity,
        String title,
        String viewMode,
        List<MonthColumnDto> columns,
        List<ReportRowDto> rows,
        List<ChartPointDto> trend,
        List<ReportDetailDto> details,
        List<ReportCalendarDayDto> calendarDays,
        List<String> footnotes) {}
