package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.report.MonthlyReportResponse;
import com.pininicong.cashbook.service.MonthlyReportService;
import com.pininicong.cashbook.service.MonthlyReportService.ReportPeriod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    public MonthlyReportController(MonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
    }

    /** 월간 보고서 — 월별 컬럼 + 구분별 행 + 지출 추이(차트) */
    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(
            @RequestParam int year,
            @RequestParam(defaultValue = "H1") String period) {
        ReportPeriod p = MonthlyReportService.parsePeriod(period);
        return monthlyReportService.build(year, p);
    }
}
