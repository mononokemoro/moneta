package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.repo.CbCategoryRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.MonetaImportService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MonetaImportTest {

    private static final Path MIGA_ROOT = Path.of("c:/Users/user/Downloads/miga/dylbs");

    @Autowired MonetaImportService importService;
    @Autowired CbTransactionRepository txRepo;
    @Autowired CbCategoryRepository categoryRepo;

    @Test
    void importMonetaReportsFromDownloads() {
        if (!MIGA_ROOT.resolve("지출").toFile().isDirectory()) {
            return;
        }
        var result = importService.importFromDirectory(MIGA_ROOT, true);
        assertThat(result.expensesImported()).isGreaterThan(0);
        assertThat(result.incomesImported()).isGreaterThan(0);
        assertThat(txRepo.count()).isGreaterThan(1000);
        assertThat(categoryRepo.count()).isGreaterThan(20);
    }
}
