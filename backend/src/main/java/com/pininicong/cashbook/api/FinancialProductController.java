package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.FinancialProductDto.CardSyncFromTransactionsResponse;
import com.pininicong.cashbook.dto.FinancialProductDto.FinancialProductListResponse;
import com.pininicong.cashbook.dto.FinancialProductDto.FinancialProductSaveRequest;
import com.pininicong.cashbook.service.FinancialProductService;
import com.pininicong.cashbook.support.LedgerBookParser;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial-products")
@Validated
public class FinancialProductController {

    private final FinancialProductService productService;

    public FinancialProductController(FinancialProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public FinancialProductListResponse list(@RequestParam(defaultValue = "PERSONAL") String book) {
        return productService.list(LedgerBookParser.parse(book));
    }

    @PutMapping
    public FinancialProductListResponse save(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody FinancialProductSaveRequest req) {
        return productService.save(LedgerBookParser.parse(book), req);
    }

    @PostMapping("/sync-cards-from-transactions")
    public CardSyncFromTransactionsResponse syncCardsFromTransactions(
            @RequestParam(defaultValue = "PERSONAL") String book) {
        return productService.syncCardsFromTransactions(LedgerBookParser.parse(book));
    }
}
