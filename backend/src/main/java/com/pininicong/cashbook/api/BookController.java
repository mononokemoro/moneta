package com.pininicong.cashbook.api;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.dto.BookDto;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookController {

    @GetMapping("/books")
    public List<BookDto> books() {
        return Arrays.stream(LedgerBook.values())
                .map(b -> new BookDto(b.name(), b.label()))
                .toList();
    }
}
