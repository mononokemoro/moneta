package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.BudgetUpdateRequest;
import com.pininicong.cashbook.dto.CalendarMarkersDto;
import com.pininicong.cashbook.dto.CashBalanceUpdateRequest;
import com.pininicong.cashbook.dto.CreatedTransactionDto;
import com.pininicong.cashbook.dto.DailySheetUpdateRequest;
import com.pininicong.cashbook.dto.DayTransactionTableResponse;
import com.pininicong.cashbook.dto.DayViewDto;
import com.pininicong.cashbook.dto.FixedItemSendRequest;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionMoveRequest;
import com.pininicong.cashbook.dto.TransactionUpdateRequest;
import com.pininicong.cashbook.service.CashbookService;
import com.pininicong.cashbook.support.LedgerBookParser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
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
    public DayViewDto day(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "PERSONAL") String book) {
        return cashbookService.getDay(date, LedgerBookParser.parse(book));
    }

    @GetMapping("/day/transactions/table")
    public DayTransactionTableResponse dayTransactionTable(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "PERSONAL") String book) {
        return cashbookService.getDayTransactionTable(date, LedgerBookParser.parse(book));
    }

    @GetMapping("/calendar-markers")
    public CalendarMarkersDto calendarMarkers(
            @RequestParam String yearMonth, @RequestParam(defaultValue = "PERSONAL") String book) {
        return cashbookService.getCalendarMarkers(LedgerBookParser.parse(book), YearMonth.parse(yearMonth));
    }

    @PostMapping("/transactions")
    public CreatedTransactionDto create(@Valid @RequestBody TransactionCreateRequest req) {
        return cashbookService.createTransaction(req);
    }

    @DeleteMapping("/transactions/{id}")
    public void delete(@PathVariable Long id) {
        cashbookService.deleteTransaction(id);
    }

    @PutMapping("/transactions/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody TransactionUpdateRequest req) {
        cashbookService.updateTransaction(id, req);
    }

    @PostMapping("/transactions/move")
    public void move(@Valid @RequestBody TransactionMoveRequest req) {
        cashbookService.moveTransactions(req);
    }

    @PostMapping("/fixed-items/send-to-cashbook")
    public int sendFixedItems(@Valid @RequestBody FixedItemSendRequest req) {
        return cashbookService.sendFixedItemsToCashbook(req);
    }

    @PutMapping("/budget/{yearMonth}")
    public void budget(
            @PathVariable String yearMonth,
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody BudgetUpdateRequest req) {
        cashbookService.upsertBudget(LedgerBookParser.parse(book), yearMonth, req);
    }

    @PutMapping("/cash-balance")
    public void cashBalance(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody CashBalanceUpdateRequest req) {
        cashbookService.updateCashBalance(LedgerBookParser.parse(book), req);
    }

    @PutMapping("/day/{date}/sheet")
    public void sheet(
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "PERSONAL") String book,
            @RequestBody DailySheetUpdateRequest req) {
        cashbookService.upsertDailySheet(LedgerBookParser.parse(book), date, req);
    }
}
