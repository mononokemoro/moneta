package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbBookSettings;
import com.pininicong.cashbook.domain.ItemFieldMode;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.dto.BookSettingsDto;
import com.pininicong.cashbook.dto.BookSettingsDto.BookSettingsUpdateRequest;
import com.pininicong.cashbook.repo.CbBookSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookSettingsService {

    private final CbBookSettingsRepository settingsRepo;

    public BookSettingsService(CbBookSettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    public BookSettingsDto get(LedgerBook book) {
        return toDto(requireSettings(bookOrDefault(book)));
    }

    @Transactional
    public BookSettingsDto update(LedgerBook book, BookSettingsUpdateRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        CbBookSettings row = requireSettings(ledger);
        row.setExpenseFieldMode(req.expenseFieldMode());
        row.setSavingsInsuranceFieldMode(req.savingsInsuranceFieldMode());
        row.setLoanFieldMode(req.loanFieldMode());
        row.setIncomeFieldMode(req.incomeFieldMode());
        settingsRepo.save(row);
        return toDto(row);
    }

    private CbBookSettings requireSettings(LedgerBook book) {
        return settingsRepo
                .findById(book)
                .orElseGet(
                        () -> {
                            CbBookSettings created = new CbBookSettings();
                            created.setBook(book);
                            return settingsRepo.save(created);
                        });
    }

    private static LedgerBook bookOrDefault(LedgerBook book) {
        return book != null ? book : LedgerBook.PERSONAL;
    }

    private static BookSettingsDto toDto(CbBookSettings row) {
        return new BookSettingsDto(
                row.getExpenseFieldMode(),
                row.getSavingsInsuranceFieldMode(),
                row.getLoanFieldMode(),
                row.getIncomeFieldMode());
    }
}
