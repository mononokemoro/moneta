package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.report.ChartPointDto;
import com.pininicong.cashbook.dto.report.MonthColumnDto;
import com.pininicong.cashbook.dto.report.MonthlyReportResponse;
import com.pininicong.cashbook.dto.report.ReportRowDto;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MonthlyReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public enum ReportPeriod {
        H1,
        H2,
        FY
    }

    private final CbTransactionRepository txRepo;

    public MonthlyReportService(CbTransactionRepository txRepo) {
        this.txRepo = txRepo;
    }

    public MonthlyReportResponse build(int year, ReportPeriod period) {
        List<YearMonth> months = monthsFor(year, period);
        LocalDate start = months.getFirst().atDay(1);
        LocalDate end = months.getLast().atEndOfMonth();

        List<CbTransaction> txs = txRepo.findByTxDateBetween(start, end);

        List<MonthColumnDto> columns = new ArrayList<>();
        DateTimeFormatter rf = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA);
        for (YearMonth ym : months) {
            LocalDate first = ym.atDay(1);
            LocalDate last = ym.atEndOfMonth();
            String range = first.format(rf) + "~" + last.format(rf);
            columns.add(
                    new MonthColumnDto(
                            ym.toString(),
                            ym.getMonthValue() + "월",
                            range));
        }

        Predicate<CbTransaction> income = t -> t.getTxType() == TxType.INCOME;
        Predicate<CbTransaction> savingsPure =
                t ->
                        t.getTxType() == TxType.SAVINGS
                                && !containsInsurance(cat(t));
        Predicate<CbTransaction> insurance =
                t -> t.getTxType() == TxType.SAVINGS && containsInsurance(cat(t));
        Predicate<CbTransaction> loan = t -> t.getTxType() == TxType.EXPENSE && containsLoan(cat(t));
        Predicate<CbTransaction> expenseNet =
                t -> t.getTxType() == TxType.EXPENSE && !containsLoan(cat(t));

        List<ReportRowDto> rows =
                List.of(
                        row("INCOME", "수입", months, txs, income),
                        row("SAVINGS", "저축", months, txs, savingsPure),
                        row("INSURANCE", "보험", months, txs, insurance),
                        row("LOAN", "대출", months, txs, loan),
                        row("EXPENSE", "지출", months, txs, expenseNet));

        List<BigDecimal> expenseValues =
                rows.stream()
                        .filter(r -> "EXPENSE".equals(r.key()))
                        .findFirst()
                        .map(ReportRowDto::values)
                        .orElse(List.of());

        List<ChartPointDto> trend = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            YearMonth ym = months.get(i);
            BigDecimal v = i < expenseValues.size() ? expenseValues.get(i) : ZERO;
            long longVal = v.setScale(0, RoundingMode.HALF_UP).longValue();
            trend.add(new ChartPointDto(ym.toString(), ym.getMonthValue() + "월", longVal));
        }

        String periodLabel =
                switch (period) {
                    case H1 -> "상반기";
                    case H2 -> "하반기";
                    case FY -> "전체";
                };

        return new MonthlyReportResponse(year, period.name(), periodLabel, columns, rows, trend);
    }

    private static ReportRowDto row(
            String key,
            String label,
            List<YearMonth> months,
            List<CbTransaction> txs,
            Predicate<CbTransaction> filter) {
        List<BigDecimal> values = new ArrayList<>();
        BigDecimal total = ZERO;
        for (YearMonth ym : months) {
            BigDecimal sum = sumMonth(txs, ym, filter);
            values.add(sum);
            total = total.add(sum);
        }
        return new ReportRowDto(key, label, values, total);
    }

    private static BigDecimal sumMonth(
            List<CbTransaction> txs, YearMonth ym, Predicate<CbTransaction> filter) {
        return txs.stream()
                .filter(t -> YearMonth.from(t.getTxDate()).equals(ym))
                .filter(filter)
                .map(CbTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private static String cat(CbTransaction t) {
        return t.getCategory() != null ? t.getCategory() : "";
    }

    private static boolean containsInsurance(String category) {
        return category.contains("보험");
    }

    private static boolean containsLoan(String category) {
        return category.contains("대출");
    }

    private static List<YearMonth> monthsFor(int year, ReportPeriod period) {
        return switch (period) {
            case H1 ->
                    java.util.stream.IntStream.rangeClosed(1, 6)
                            .mapToObj(m -> YearMonth.of(year, m))
                            .toList();
            case H2 ->
                    java.util.stream.IntStream.rangeClosed(7, 12)
                            .mapToObj(m -> YearMonth.of(year, m))
                            .toList();
            case FY ->
                    java.util.stream.IntStream.rangeClosed(1, 12)
                            .mapToObj(m -> YearMonth.of(year, m))
                            .toList();
        };
    }

    /** 쿼리 파라미터 파싱용 */
    public static ReportPeriod parsePeriod(String raw) {
        if (raw == null || raw.isBlank()) {
            return ReportPeriod.H1;
        }
        try {
            return ReportPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ReportPeriod.H1;
        }
    }
}
