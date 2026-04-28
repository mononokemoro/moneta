package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.BudgetUpdateRequest;
import com.pininicong.cashbook.dto.CashBalanceUpdateRequest;
import com.pininicong.cashbook.dto.CreatedTransactionDto;
import com.pininicong.cashbook.dto.DailySheetUpdateRequest;
import com.pininicong.cashbook.dto.DayViewDto;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.service.CashbookService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
public class CashbookController {

    private final CashbookService cashbookService;

    public CashbookController(CashbookService cashbookService) {
        this.cashbookService = cashbookService;
    }

    @GetMapping("/day")
    public DayViewDto day(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return cashbookService.getDay(date);
    }

    @PostMapping("/transactions")
    public CreatedTransactionDto create(@Valid @RequestBody TransactionCreateRequest req) {
        return cashbookService.createTransaction(req);
    }

    @DeleteMapping("/transactions/{id}")
    public void delete(@PathVariable Long id) {
        cashbookService.deleteTransaction(id);
    }

    @PutMapping("/budget/{yearMonth}")
    public void budget(
            @PathVariable String yearMonth, @Valid @RequestBody BudgetUpdateRequest req) {
        cashbookService.upsertBudget(yearMonth, req);
    }

    @PutMapping("/cash-balance")
    public void cashBalance(@Valid @RequestBody CashBalanceUpdateRequest req) {
        cashbookService.updateCashBalance(req);
    }

    @PutMapping("/day/{date}/sheet")
    public void sheet(
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestBody DailySheetUpdateRequest req) {
        cashbookService.upsertDailySheet(date, req);
    }
}
