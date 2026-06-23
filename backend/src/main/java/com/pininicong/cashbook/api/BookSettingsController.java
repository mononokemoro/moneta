package com.pininicong.cashbook.api;

import com.pininicong.cashbook.dto.BookSettingsDto;
import com.pininicong.cashbook.dto.BookSettingsDto.BookSettingsUpdateRequest;
import com.pininicong.cashbook.service.BookSettingsService;
import com.pininicong.cashbook.support.LedgerBookParser;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book-settings")
@Validated
public class BookSettingsController {

    private final BookSettingsService settingsService;

    public BookSettingsController(BookSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public BookSettingsDto get(@RequestParam(defaultValue = "PERSONAL") String book) {
        return settingsService.get(LedgerBookParser.parse(book));
    }

    @PutMapping
    public BookSettingsDto update(
            @RequestParam(defaultValue = "PERSONAL") String book,
            @Valid @RequestBody BookSettingsUpdateRequest req) {
        return settingsService.update(LedgerBookParser.parse(book), req);
    }
}
