package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.CategoryCreateRequest;
import com.pininicong.cashbook.dto.CategoryDto;
import com.pininicong.cashbook.dto.CategoryPreferencesRequest;
import com.pininicong.cashbook.dto.CategoryReorderRequest;
import com.pininicong.cashbook.dto.CategoryUpdateRequest;
import com.pininicong.cashbook.service.CategoryService;
import com.pininicong.cashbook.support.LedgerBookParser;
import jakarta.validation.Valid;
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
@RequestMapping("/api/categories")
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public CategoryDto.CategoryListResponse list(
            @RequestParam(defaultValue = "PERSONAL") String book) {
        return categoryService.listCategories(LedgerBookParser.parse(book));
    }

    @PostMapping
    public CategoryDto create(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody CategoryCreateRequest req) {
        return categoryService.create(LedgerBookParser.parse(book), req);
    }

    @PutMapping("/{id}")
    public CategoryDto update(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody CategoryUpdateRequest req) {
        return categoryService.update(id, LedgerBookParser.parse(book), req);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id, @RequestParam(defaultValue = "PERSONAL") String book) {
        categoryService.delete(id, LedgerBookParser.parse(book));
    }

    @GetMapping("/{id}/transactions")
    public CategoryDto.CategoryTransactionsResponse listTransactions(
            @PathVariable Long id, @RequestParam(defaultValue = "PERSONAL") String book) {
        return categoryService.listTransactionsForCategory(id, LedgerBookParser.parse(book));
    }

    @GetMapping("/{id}/transactions/table")
    public CategoryDto.CategoryTransactionTableResponse listTransactionTable(
            @PathVariable Long id, @RequestParam(defaultValue = "PERSONAL") String book) {
        return categoryService.listTransactionTableForCategory(id, LedgerBookParser.parse(book));
    }

    @PutMapping("/reorder")
    public void reorder(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody CategoryReorderRequest req) {
        categoryService.reorder(LedgerBookParser.parse(book), req);
    }

    @PutMapping("/preferences")
    public void savePreferences(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody CategoryPreferencesRequest req) {
        categoryService.savePreferences(LedgerBookParser.parse(book), req);
    }
}
