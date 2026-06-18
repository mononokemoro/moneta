package com.pininicong.cashbook.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.imports.MonetaHtmlReportParser.ReportKind;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MonetaHtmlReportParserTest {

    @Test
    void parsesExpenseSampleFromDownloads() {
        Path file =
                Path.of(
                        "c:/Users/user/Downloads/miga/dylbs/지출/미가스마트_보고서_지출_상세내역_(20260101~20260630).xls");
        if (!file.toFile().exists()) return;
        var rows = MonetaHtmlReportParser.parse(file, ReportKind.EXPENSE);
        assertThat(rows).hasSizeGreaterThan(500);
        assertThat(rows.get(0).category()).isEqualTo("가계:환급");
    }
}
