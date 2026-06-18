package com.pininicong.cashbook.api;

import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.CategoryKeywordDto;
import com.pininicong.cashbook.dto.CategoryKeywordUpsertRequest;
import com.pininicong.cashbook.service.CategoryKeywordService;
import com.pininicong.cashbook.support.LedgerBookParser;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/category-keywords")
@Validated
public class CategoryKeywordController {

    private final CategoryKeywordService service;

    public CategoryKeywordController(CategoryKeywordService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryKeywordDto> list(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @RequestParam(required = false) TxType txType) {
        if (txType != null) {
            return service.list(LedgerBookParser.parse(book), txType);
        }
        return service.listAll(LedgerBookParser.parse(book));
    }

    @PostMapping
    public CategoryKeywordDto create(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody CategoryKeywordUpsertRequest req) {
        return service.create(LedgerBookParser.parse(book), req);
    }

    @PutMapping("/{id}")
    public CategoryKeywordDto update(
            @PathVariable Long id, @Valid @RequestBody CategoryKeywordUpsertRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
