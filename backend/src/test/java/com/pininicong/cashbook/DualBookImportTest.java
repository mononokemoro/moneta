package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.MonetaImportService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DualBookImportTest {

    private static final Path PERSONAL_ROOT = Path.of("c:/Users/user/Downloads/miga/dylbs");
    private static final Path HOUSEHOLD_ROOT = Path.of("c:/Users/user/Downloads/miga/gaegye");

    @Autowired MonetaImportService importService;
    @Autowired CbTransactionRepository txRepo;

    @Test
    void householdImportDoesNotRemovePersonalBook() {
        if (!PERSONAL_ROOT.resolve("지출").toFile().isDirectory()) {
            return;
        }
        importService.importFromDirectory(PERSONAL_ROOT, true, "*", LedgerBook.PERSONAL);
        long personalCount = txRepo.findByBookAndTxDateBetween(
                        LedgerBook.PERSONAL,
                        java.time.LocalDate.of(2010, 1, 1),
                        java.time.LocalDate.of(2030, 12, 31))
                .size();
        assertThat(personalCount).isGreaterThan(1000);

        if (!HOUSEHOLD_ROOT.resolve("지출").toFile().isDirectory()) {
            return;
        }
        var householdResult = importService.importFromDirectory(HOUSEHOLD_ROOT, true, "*", LedgerBook.HOUSEHOLD);
        assertThat(householdResult.expensesImported()).isGreaterThan(0);

        long personalAfter = txRepo.findByBookAndTxDateBetween(
                        LedgerBook.PERSONAL,
                        java.time.LocalDate.of(2010, 1, 1),
                        java.time.LocalDate.of(2030, 12, 31))
                .size();
        assertThat(personalAfter).isEqualTo(personalCount);

        long householdCount = txRepo.findByBookAndTxDateBetween(
                        LedgerBook.HOUSEHOLD,
                        java.time.LocalDate.of(2010, 1, 1),
                        java.time.LocalDate.of(2030, 12, 31))
                .size();
        assertThat(householdCount).isGreaterThan(0);
    }
}
