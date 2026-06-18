package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.report.MonthlyReportResponse;
import com.pininicong.cashbook.dto.report.ReportResponse;
import com.pininicong.cashbook.service.ReportService;
import com.pininicong.cashbook.service.ReportService.ReportGranularity;
import com.pininicong.cashbook.service.ReportService.ReportPeriod;
import com.pininicong.cashbook.support.LedgerBookParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class MonthlyReportController {

    private final ReportService reportService;

    public MonthlyReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** @deprecated 호환용 — {@link #report} 사용 권장 */
    @GetMapping("/monthly")
    public MonthlyReportResponse monthly(
            @RequestParam int year,
            @RequestParam(defaultValue = "H1") String period,
            @RequestParam(defaultValue = "PERSONAL") String book) {
        ReportPeriod p = ReportService.parsePeriod(period);
        return reportService.buildMonthlyLegacy(year, p, LedgerBookParser.parse(book));
    }

    /** 통합 보고서 (전체·수입·지출·카드·예산·달력 등) */
    @GetMapping
    public ReportResponse report(
            @RequestParam int year,
            @RequestParam(defaultValue = "H1") String period,
            @RequestParam(defaultValue = "PERSONAL") String book,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "MONTHLY") String subView,
            @RequestParam(defaultValue = "MONTHLY") String granularity) {
        ReportPeriod p = ReportService.parsePeriod(period);
        ReportGranularity g = ReportService.parseGranularity(granularity);
        return reportService.build(year, p, LedgerBookParser.parse(book), category, subView, g);
    }
}
