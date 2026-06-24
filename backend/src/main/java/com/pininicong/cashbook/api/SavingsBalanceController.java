package com.pininicong.cashbook.api;

import com.pininicong.cashbook.domain.CbProductBalanceAnchor;
import com.pininicong.cashbook.domain.CbProductPeriodSummary;
import com.pininicong.cashbook.dto.ProductBalanceAnchorDto;
import com.pininicong.cashbook.dto.ProductBalanceAnchorUpsertRequest;
import com.pininicong.cashbook.dto.ProductPeriodSummaryDto;
import com.pininicong.cashbook.service.ProductPeriodSummaryService;
import com.pininicong.cashbook.service.SavingsBalanceService;
import com.pininicong.cashbook.service.TransactionSavingsProductSupport;
import com.pininicong.cashbook.support.LedgerBookParser;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/savings-balance")
@Validated
public class SavingsBalanceController {

    private final SavingsBalanceService balanceService;
    private final ProductPeriodSummaryService periodSummaryService;
    private final TransactionSavingsProductSupport savingsProductSupport;

    public SavingsBalanceController(
            SavingsBalanceService balanceService,
            ProductPeriodSummaryService periodSummaryService,
            TransactionSavingsProductSupport savingsProductSupport) {
        this.balanceService = balanceService;
        this.periodSummaryService = periodSummaryService;
        this.savingsProductSupport = savingsProductSupport;
    }

    @PutMapping("/anchor")
    public ProductBalanceAnchorDto upsertAnchor(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody ProductBalanceAnchorUpsertRequest req) {
        CbProductBalanceAnchor anchor =
                balanceService.upsertAnchor(
                        LedgerBookParser.parse(book),
                        req.productId(),
                        req.title(),
                        req.anchorDate(),
                        req.balance(),
                        req.remarks());
        String productName =
                savingsProductSupport
                        .name(LedgerBookParser.parse(book), anchor.getProductId());
        return new ProductBalanceAnchorDto(
                anchor.getId(),
                anchor.getProductId(),
                productName,
                anchor.getAnchorDate(),
                anchor.getBalance(),
                anchor.getRemarks());
    }

    @GetMapping("/period-summary")
    public List<ProductPeriodSummaryDto> periodSummary(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @RequestParam String yearMonth) {
        var ledger = LedgerBookParser.parse(book);
        var ym = YearMonth.parse(yearMonth);
        var rows = periodSummaryService.refreshMonth(ledger, ym);
        return rows.stream().map(this::toPeriodDto).toList();
    }

    private ProductPeriodSummaryDto toPeriodDto(CbProductPeriodSummary row) {
        return new ProductPeriodSummaryDto(
                row.getProductId(),
                savingsProductSupport.name(row.getBook(), row.getProductId()),
                row.getPeriodType().name(),
                row.getPeriodKey(),
                row.getInflow(),
                row.getOutflow(),
                row.getNetFlow(),
                row.getEndBalance());
    }
}
