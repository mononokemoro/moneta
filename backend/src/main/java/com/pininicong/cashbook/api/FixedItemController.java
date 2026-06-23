package com.pininicong.cashbook.api;

import com.pininicong.cashbook.domain.FixedKind;
import com.pininicong.cashbook.domain.FixedScheduleType;
import com.pininicong.cashbook.dto.FixedItemDto;
import com.pininicong.cashbook.service.FixedItemService;
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
@RequestMapping("/api/fixed-items")
@Validated
public class FixedItemController {

    private final FixedItemService fixedItemService;

    public FixedItemController(FixedItemService fixedItemService) {
        this.fixedItemService = fixedItemService;
    }

    @GetMapping
    public FixedItemDto.FixedItemListResponse list(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @RequestParam(required = false) FixedKind kind,
            @RequestParam(required = false) FixedScheduleType scheduleType) {
        return fixedItemService.list(LedgerBookParser.parse(book), kind, scheduleType);
    }

    @PostMapping
    public FixedItemDto.FixedItemRow create(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody FixedItemDto.FixedItemSaveRequest req) {
        return fixedItemService.create(LedgerBookParser.parse(book), req);
    }

    @PutMapping("/{id}")
    public FixedItemDto.FixedItemRow update(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody FixedItemDto.FixedItemSaveRequest req) {
        return fixedItemService.update(id, LedgerBookParser.parse(book), req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam(defaultValue = "PERSONAL") String book) {
        fixedItemService.delete(id, LedgerBookParser.parse(book));
    }

    @PostMapping("/bulk-delete")
    public void bulkDelete(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody FixedItemDto.FixedItemBulkDeleteRequest req) {
        fixedItemService.deleteMany(LedgerBookParser.parse(book), req.ids());
    }
}
