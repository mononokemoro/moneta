package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.MonetaImportService;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** 로컬 H2 파일 DB(%USERPROFILE%/.pininicong-cashbook)에 모네타 데이터를 적재합니다. */
@Disabled("로컬 H2 DB를 덮어씁니다. 서버 종료 후 수동 실행하세요.")
@SpringBootTest
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:file:${user.home}/.pininicong-cashbook/db;DB_CLOSE_ON_EXIT=FALSE",
            "spring.jpa.hibernate.ddl-auto=update",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
        })
class MonetaImportLiveTest {

    private static final Path MIGA_ROOT = Path.of("c:/Users/user/Downloads/miga/dylbs");

    @Autowired MonetaImportService importService;
    @Autowired CbTransactionRepository txRepo;

    @Test
    void importToLocalFileDatabase() {
        if (!MIGA_ROOT.resolve("지출").toFile().isDirectory()) {
            return;
        }
        var result = importService.importFromDirectory(MIGA_ROOT, true, "*", com.pininicong.cashbook.domain.LedgerBook.PERSONAL);
        assertThat(result.expensesImported()).isGreaterThan(5000);
        assertThat(result.incomesImported()).isGreaterThan(1000);
        assertThat(result.savingsImported()).isGreaterThan(5000);
        assertThat(txRepo.count()).isGreaterThan(10000);
    }
}
