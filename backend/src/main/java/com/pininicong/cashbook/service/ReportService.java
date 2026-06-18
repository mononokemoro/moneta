package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbMonthlyBudget;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.MonthlyBudgetKey;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.report.ChartPointDto;
import com.pininicong.cashbook.dto.report.MonthColumnDto;
import com.pininicong.cashbook.dto.report.MonthlyReportResponse;
import com.pininicong.cashbook.dto.report.ReportCalendarDayDto;
import com.pininicong.cashbook.dto.report.ReportDetailDto;
import com.pininicong.cashbook.dto.report.ReportResponse;
import com.pininicong.cashbook.dto.report.ReportRowDto;
import com.pininicong.cashbook.repo.CbMonthlyBudgetRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter RF = DateTimeFormatter.ofPattern("MM.dd", Locale.KOREA);
    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    public enum ReportPeriod {
        H1,
        H2,
        FY
    }

    public enum ReportGranularity {
        MONTHLY,
        WEEKLY,
        DAILY
    }

    private final CbTransactionRepository txRepo;
    private final CbMonthlyBudgetRepository budgetRepo;

    public ReportService(CbTransactionRepository txRepo, CbMonthlyBudgetRepository budgetRepo) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
    }

    public MonthlyReportResponse buildMonthlyLegacy(int year, ReportPeriod period, LedgerBook book) {
        ReportResponse r = build(year, period, book, "ALL", "MONTHLY", ReportGranularity.MONTHLY);
        return new MonthlyReportResponse(
                r.year(), r.period(), r.periodLabel(), r.columns(), r.rows(), r.trend());
    }

    public ReportResponse build(
            int year,
            ReportPeriod period,
            LedgerBook book,
            String categoryRaw,
            String subViewRaw,
            ReportGranularity granularity) {
        LedgerBook ledger = book != null ? book : LedgerBook.PERSONAL;
        String category = normalize(categoryRaw, "ALL");
        String subView = normalize(subViewRaw, defaultSubView(category));
        ReportGranularity g = resolveGranularity(category, subView, granularity);

        LocalDate start = periodStart(year, period);
        LocalDate end = periodEnd(year, period);
        List<CbTransaction> txs = txRepo.findByBookAndTxDateBetween(ledger, start, end);
        String periodLabel = periodLabel(period);

        if (isCalendar(category)) {
            return buildCalendar(year, period, periodLabel, ledger, category, subView, start, end, txs);
        }
        if (isDetailView(category, subView)) {
            return buildDetail(year, period, periodLabel, ledger, category, subView, g, txs);
        }
        if ("BUDGET".equals(category)) {
            return buildBudget(year, period, periodLabel, ledger, subView, start, end, txs);
        }
        if ("CARD".equals(category)) {
            return buildCardMatrix(year, period, periodLabel, ledger, subView, g, start, end, txs);
        }

        List<TimeBucket> buckets = bucketsFor(start, end, g);
        List<MonthColumnDto> columns = toColumns(buckets, g);
        List<ReportRowDto> rows = matrixRows(category, subView, buckets, txs);
        List<ChartPointDto> trend = trendFromRows(rows, buckets, primaryTrendKey(category, subView));
        String title = buildTitle(year, periodLabel, ledger, category, subView, g);
        return new ReportResponse(
                year,
                period.name(),
                periodLabel,
                category,
                subView,
                g.name(),
                title,
                "matrix",
                columns,
                rows,
                trend,
                List.of(),
                List.of(),
                footnotes(category, subView));
    }

    private ReportResponse buildDetail(
            int year,
            ReportPeriod period,
            String periodLabel,
            LedgerBook ledger,
            String category,
            String subView,
            ReportGranularity granularity,
            List<CbTransaction> txs) {
        Predicate<CbTransaction> filter = detailFilter(category, subView);
        List<ReportDetailDto> details =
                txs.stream()
                        .filter(filter)
                        .sorted(
                                Comparator.comparing(CbTransaction::getTxDate)
                                        .reversed()
                                        .thenComparing(CbTransaction::getId))
                        .map(this::toDetail)
                        .toList();
        String title = buildTitle(year, periodLabel, ledger, category, subView, granularity) + " · 상세내역";
        return new ReportResponse(
                year,
                period.name(),
                periodLabel,
                category,
                subView,
                granularity.name(),
                title,
                "detail",
                List.of(),
                List.of(),
                List.of(),
                details,
                List.of(),
                footnotes(category, subView));
    }

    private ReportResponse buildCalendar(
            int year,
            ReportPeriod period,
            String periodLabel,
            LedgerBook ledger,
            String category,
            String subView,
            LocalDate start,
            LocalDate end,
            List<CbTransaction> txs) {
        Predicate<CbTransaction> base = calendarBaseFilter(subView);
        Map<LocalDate, BigDecimal[]> byDay = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            byDay.put(d, new BigDecimal[] {ZERO, ZERO, ZERO});
        }
        for (CbTransaction t : txs) {
            if (!base.test(t)) continue;
            BigDecimal[] cell = byDay.get(t.getTxDate());
            if (cell == null) continue;
            BigDecimal a = n(t.getAmount());
            cell[0] = cell[0].add(a);
            if (isCardExpense(t)) {
                cell[2] = cell[2].add(a);
            } else if (t.getTxType() == TxType.EXPENSE) {
                cell[1] = cell[1].add(a);
            }
        }
        List<ReportCalendarDayDto> days = new ArrayList<>();
        for (var e : byDay.entrySet()) {
            BigDecimal[] c = e.getValue();
            days.add(
                    new ReportCalendarDayDto(
                            e.getKey().format(DF),
                            toLong(c[0]),
                            toLong(c[1]),
                            toLong(c[2])));
        }
        String title = year + "년 · " + ledger.label() + " · " + periodLabel + " · 달력보기";
        return new ReportResponse(
                year,
                period.name(),
                periodLabel,
                category,
                subView,
                ReportGranularity.DAILY.name(),
                title,
                "calendar",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                days,
                footnotes(category, subView));
    }

    private ReportResponse buildBudget(
            int year,
            ReportPeriod period,
            String periodLabel,
            LedgerBook ledger,
            String subView,
            LocalDate start,
            LocalDate end,
            List<CbTransaction> txs) {
        List<YearMonth> months = monthsBetween(start, end);
        List<MonthColumnDto> columns = monthColumns(months);
        List<BigDecimal> budgets = new ArrayList<>();
        List<BigDecimal> spent = new ArrayList<>();
        for (YearMonth ym : months) {
            BigDecimal b =
                    budgetRepo
                            .findById(new MonthlyBudgetKey(ledger, ym.toString()))
                            .map(CbMonthlyBudget::getTotalBudget)
                            .orElse(ZERO);
            budgets.add(n(b));
            spent.add(sumMonth(txs, ym, t -> t.getTxType() == TxType.EXPENSE && !containsLoan(cat(t))));
        }
        List<ReportRowDto> rows;
        if ("BUDGET_WRITE".equals(subView)) {
            rows = List.of(rowFromValues("BUDGET", "총예산", budgets));
        } else {
            List<BigDecimal> diff = new ArrayList<>();
            BigDecimal tb = ZERO;
            BigDecimal ts = ZERO;
            for (int i = 0; i < months.size(); i++) {
                diff.add(budgets.get(i).subtract(spent.get(i)));
                tb = tb.add(budgets.get(i));
                ts = ts.add(spent.get(i));
            }
            rows =
                    List.of(
                            new ReportRowDto("BUDGET", "총예산", budgets, tb),
                            new ReportRowDto("SPENT", "실적(지출)", spent, ts),
                            new ReportRowDto("DIFF", "차이(예산-지출)", diff, tb.subtract(ts)));
        }
        String title = year + "년 · " + ledger.label() + " · " + periodLabel + " · 예산/실적";
        return new ReportResponse(
                year,
                period.name(),
                periodLabel,
                "BUDGET",
                subView,
                ReportGranularity.MONTHLY.name(),
                title,
                "matrix",
                columns,
                rows,
                trendFromRowValues(spent, months),
                List.of(),
                List.of(),
                footnotes("BUDGET", subView));
    }

    private ReportResponse buildCardMatrix(
            int year,
            ReportPeriod period,
            String periodLabel,
            LedgerBook ledger,
            String subView,
            ReportGranularity g,
            LocalDate start,
            LocalDate end,
            List<CbTransaction> txs) {
        List<CbTransaction> cardTxs = txs.stream().filter(this::isCardExpense).toList();

        if ("BY_CARD_NAME".equals(subView)) {
            List<YearMonth> months = monthsBetween(start, end);
            List<MonthColumnDto> columns = monthColumns(months);
            Map<String, List<CbTransaction>> byCard =
                    cardTxs.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            t ->
                                                    isBlank(t.getCardName())
                                                            ? "(미지정)"
                                                            : t.getCardName(),
                                            LinkedHashMap::new,
                                            Collectors.toList()));
            List<ReportRowDto> rows = new ArrayList<>();
            for (var e : byCard.entrySet()) {
                List<BigDecimal> vals = new ArrayList<>();
                BigDecimal total = ZERO;
                for (YearMonth ym : months) {
                    BigDecimal s = sumMonth(e.getValue(), ym, t -> true);
                    vals.add(s);
                    total = total.add(s);
                }
                rows.add(new ReportRowDto("CARD:" + e.getKey(), e.getKey(), vals, total));
            }
            List<TimeBucket> buckets = new ArrayList<>();
            for (YearMonth ym : months) {
                LocalDate s = ym.atDay(1);
                LocalDate e = ym.atEndOfMonth();
                buckets.add(
                        new TimeBucket(
                                s, e, ym.getMonthValue() + "월", s.format(RF) + "~" + e.format(RF)));
            }
            String trendKey = rows.isEmpty() ? "CARD" : rows.get(0).key();
            return matrixResponse(
                    year,
                    period,
                    periodLabel,
                    ledger,
                    "CARD",
                    subView,
                    ReportGranularity.MONTHLY,
                    buckets,
                    columns,
                    rows,
                    trendKey);
        }

        if ("BY_PAYMENT_DATE".equals(subView) || "DETAIL".equals(subView)) {
            List<ReportDetailDto> details =
                    cardTxs.stream()
                            .sorted(
                                    Comparator.comparing(CbTransaction::getTxDate)
                                            .reversed()
                                            .thenComparing(CbTransaction::getId))
                            .map(this::toDetail)
                            .toList();
            return new ReportResponse(
                    year,
                    period.name(),
                    periodLabel,
                    "CARD",
                    subView,
                    g.name(),
                    year + "년 · " + ledger.label() + " · " + periodLabel + " · 카드 상세",
                    "detail",
                    List.of(),
                    List.of(),
                    List.of(),
                    details,
                    List.of(),
                    footnotes("CARD", subView));
        }

        List<TimeBucket> buckets = bucketsFor(start, end, g);
        List<MonthColumnDto> columns = toColumns(buckets, g);
        List<BigDecimal> values = bucketSums(buckets, cardTxs, t -> true);
        BigDecimal total = values.stream().reduce(ZERO, BigDecimal::add);
        ReportRowDto row = new ReportRowDto("CARD", "카드지출", values, total);
        return matrixResponse(
                year, period, periodLabel, ledger, "CARD", subView, g, buckets, columns, List.of(row), "CARD");
    }

    private ReportResponse matrixResponse(
            int year,
            ReportPeriod period,
            String periodLabel,
            LedgerBook ledger,
            String category,
            String subView,
            ReportGranularity g,
            List<TimeBucket> buckets,
            List<MonthColumnDto> columns,
            List<ReportRowDto> rows,
            String trendKey) {
        return new ReportResponse(
                year,
                period.name(),
                periodLabel,
                category,
                subView,
                g.name(),
                buildTitle(year, periodLabel, ledger, category, subView, g),
                "matrix",
                columns,
                rows,
                trendFromRows(rows, buckets, trendKey),
                List.of(),
                List.of(),
                footnotes(category, subView));
    }

    private List<ReportRowDto> matrixRows(
            String category, String subView, List<TimeBucket> buckets, List<CbTransaction> txs) {
        Predicate<CbTransaction> income = t -> t.getTxType() == TxType.INCOME;
        Predicate<CbTransaction> savingsPure =
                t -> t.getTxType() == TxType.SAVINGS && !containsInsurance(cat(t));
        Predicate<CbTransaction> insurance =
                t -> t.getTxType() == TxType.SAVINGS && containsInsurance(cat(t));
        Predicate<CbTransaction> loan = t -> t.getTxType() == TxType.EXPENSE && containsLoan(cat(t));
        Predicate<CbTransaction> expenseNet =
                t -> t.getTxType() == TxType.EXPENSE && !containsLoan(cat(t));
        Predicate<CbTransaction> fixed =
                t ->
                        t.getTxType() == TxType.EXPENSE
                                && t.getRemarks() != null
                                && t.getRemarks().contains("고정항목");

        return switch (category) {
            case "INCOME" -> List.of(bucketRow("INCOME", "수입", buckets, txs, income));
            case "LOAN" -> List.of(bucketRow("LOAN", "대출", buckets, txs, loan));
            case "EXPENSE" ->
                    "FIXED".equals(subView)
                            ? List.of(bucketRow("FIXED", "고정지출", buckets, txs, fixed))
                            : List.of(bucketRow("EXPENSE", "지출", buckets, txs, expenseNet));
            case "SAVINGS_INSURANCE" ->
                    switch (subView) {
                        case "SAVINGS_MONTHLY" ->
                                List.of(bucketRow("SAVINGS", "저축", buckets, txs, savingsPure));
                        case "INSURANCE_MONTHLY" ->
                                List.of(bucketRow("INSURANCE", "보험", buckets, txs, insurance));
                        default ->
                                List.of(
                                        bucketRow("SAVINGS", "저축", buckets, txs, savingsPure),
                                        bucketRow("INSURANCE", "보험", buckets, txs, insurance));
                    };
            default ->
                    List.of(
                            bucketRow("INCOME", "수입", buckets, txs, income),
                            bucketRow("SAVINGS", "저축", buckets, txs, savingsPure),
                            bucketRow("INSURANCE", "보험", buckets, txs, insurance),
                            bucketRow("LOAN", "대출", buckets, txs, loan),
                            bucketRow("EXPENSE", "지출", buckets, txs, expenseNet));
        };
    }

    private ReportRowDto bucketRow(
            String key,
            String label,
            List<TimeBucket> buckets,
            List<CbTransaction> txs,
            Predicate<CbTransaction> filter) {
        List<BigDecimal> values = bucketSums(buckets, txs, filter);
        BigDecimal total = values.stream().reduce(ZERO, BigDecimal::add);
        return new ReportRowDto(key, label, values, total);
    }

    private List<BigDecimal> bucketSums(
            List<TimeBucket> buckets, List<CbTransaction> txs, Predicate<CbTransaction> filter) {
        List<BigDecimal> values = new ArrayList<>();
        for (TimeBucket b : buckets) {
            BigDecimal sum =
                    txs.stream()
                            .filter(filter)
                            .filter(t -> !t.getTxDate().isBefore(b.start()) && !t.getTxDate().isAfter(b.end()))
                            .map(CbTransaction::getAmount)
                            .reduce(ZERO, BigDecimal::add);
            values.add(sum);
        }
        return values;
    }

    private List<ChartPointDto> trendFromRows(
            List<ReportRowDto> rows, List<TimeBucket> buckets, String key) {
        ReportRowDto row =
                rows.stream().filter(r -> key.equals(r.key())).findFirst().orElse(null);
        if (row == null || buckets.isEmpty()) return List.of();
        List<ChartPointDto> trend = new ArrayList<>();
        for (int i = 0; i < buckets.size() && i < row.values().size(); i++) {
            TimeBucket b = buckets.get(i);
            trend.add(
                    new ChartPointDto(
                            b.start().format(DF),
                            b.label(),
                            toLong(row.values().get(i))));
        }
        return trend;
    }

    private List<ChartPointDto> trendFromRowValues(List<BigDecimal> values, List<YearMonth> months) {
        List<ChartPointDto> trend = new ArrayList<>();
        for (int i = 0; i < months.size() && i < values.size(); i++) {
            YearMonth ym = months.get(i);
            trend.add(new ChartPointDto(ym.toString(), ym.getMonthValue() + "월", toLong(values.get(i))));
        }
        return trend;
    }

    private ReportRowDto rowFromValues(String key, String label, List<BigDecimal> values) {
        BigDecimal total = values.stream().reduce(ZERO, BigDecimal::add);
        return new ReportRowDto(key, label, values, total);
    }

    private List<MonthColumnDto> toColumns(List<TimeBucket> buckets, ReportGranularity g) {
        List<MonthColumnDto> columns = new ArrayList<>();
        for (TimeBucket b : buckets) {
            String ym = YearMonth.from(b.start()).toString();
            if (g == ReportGranularity.MONTHLY) {
                ym = YearMonth.from(b.start()).toString();
            } else {
                ym = b.start().format(DF);
            }
            columns.add(new MonthColumnDto(ym, b.label(), b.range()));
        }
        return columns;
    }

    private List<TimeBucket> bucketsFor(LocalDate start, LocalDate end, ReportGranularity g) {
        return switch (g) {
            case MONTHLY -> {
                List<TimeBucket> list = new ArrayList<>();
                YearMonth cur = YearMonth.from(start);
                YearMonth last = YearMonth.from(end);
                while (!cur.isAfter(last)) {
                    LocalDate s = cur.atDay(1);
                    LocalDate e = cur.atEndOfMonth();
                    if (s.isBefore(start)) s = start;
                    if (e.isAfter(end)) e = end;
                    list.add(
                            new TimeBucket(
                                    s,
                                    e,
                                    cur.getMonthValue() + "월",
                                    s.format(RF) + "~" + e.format(RF)));
                    cur = cur.plusMonths(1);
                }
                yield list;
            }
            case WEEKLY -> {
                List<TimeBucket> list = new ArrayList<>();
                LocalDate d = start;
                int w = 1;
                while (!d.isAfter(end)) {
                    LocalDate we = d.plusDays(6);
                    if (we.isAfter(end)) we = end;
                    list.add(
                            new TimeBucket(
                                    d, we, w + "주", d.format(RF) + "~" + we.format(RF)));
                    d = we.plusDays(1);
                    w++;
                }
                yield list;
            }
            case DAILY -> {
                List<TimeBucket> list = new ArrayList<>();
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    String label = d.getMonthValue() + "/" + d.getDayOfMonth();
                    list.add(new TimeBucket(d, d, label, d.format(RF)));
                }
                yield list;
            }
        };
    }

    private List<MonthColumnDto> monthColumns(List<YearMonth> months) {
        List<MonthColumnDto> columns = new ArrayList<>();
        for (YearMonth ym : months) {
            LocalDate s = ym.atDay(1);
            LocalDate e = ym.atEndOfMonth();
            columns.add(
                    new MonthColumnDto(
                            ym.toString(),
                            ym.getMonthValue() + "월",
                            s.format(RF) + "~" + e.format(RF)));
        }
        return columns;
    }

    private List<YearMonth> monthsBetween(LocalDate start, LocalDate end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cur = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cur.isAfter(last)) {
            months.add(cur);
            cur = cur.plusMonths(1);
        }
        return months;
    }

    private static BigDecimal sumMonth(
            List<CbTransaction> txs, YearMonth ym, Predicate<CbTransaction> filter) {
        return txs.stream()
                .filter(t -> YearMonth.from(t.getTxDate()).equals(ym))
                .filter(filter)
                .map(CbTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private ReportDetailDto toDetail(CbTransaction t) {
        return new ReportDetailDto(
                t.getTxDate().format(DF),
                txTypeLabel(t.getTxType()),
                t.getTitle(),
                cat(t),
                t.getCardName() != null ? t.getCardName() : "",
                toLong(n(t.getAmount())),
                t.getRemarks() != null ? t.getRemarks() : "");
    }

    private String txTypeLabel(TxType type) {
        return switch (type) {
            case EXPENSE -> "지출";
            case INCOME -> "수입";
            case SAVINGS -> "저축";
        };
    }

    private Predicate<CbTransaction> detailFilter(String category, String subView) {
        if ("FIXED".equals(subView)) {
            return t ->
                    t.getTxType() == TxType.EXPENSE
                            && t.getRemarks() != null
                            && t.getRemarks().contains("고정항목");
        }
        return switch (category) {
            case "INCOME" -> t -> t.getTxType() == TxType.INCOME;
            case "LOAN" -> t -> t.getTxType() == TxType.EXPENSE && containsLoan(cat(t));
            case "EXPENSE" -> t -> t.getTxType() == TxType.EXPENSE && !containsLoan(cat(t));
            case "SAVINGS_INSURANCE" ->
                    switch (subView) {
                        case "SAVINGS_DETAIL" ->
                                t -> t.getTxType() == TxType.SAVINGS && !containsInsurance(cat(t));
                        case "INSURANCE_DETAIL" ->
                                t -> t.getTxType() == TxType.SAVINGS && containsInsurance(cat(t));
                        default -> t -> t.getTxType() == TxType.SAVINGS;
                    };
            default -> t -> true;
        };
    }

    private Predicate<CbTransaction> calendarBaseFilter(String subView) {
        return switch (subView) {
            case "CARD_ONLY" -> this::isCardExpense;
            default -> t -> t.getTxType() == TxType.EXPENSE && !containsLoan(cat(t));
        };
    }

    private boolean isCardExpense(CbTransaction t) {
        if (t.getTxType() != TxType.EXPENSE) return false;
        if (t.getCardName() != null && !t.getCardName().isBlank()) return true;
        String c = cat(t);
        return c.contains("신용") || c.contains("체크") || c.contains("카드");
    }

    private String primaryTrendKey(String category, String subView) {
        return switch (category) {
            case "INCOME" -> "INCOME";
            case "LOAN" -> "LOAN";
            case "EXPENSE" -> "FIXED".equals(subView) ? "FIXED" : "EXPENSE";
            case "SAVINGS_INSURANCE" ->
                    switch (subView) {
                        case "INSURANCE_MONTHLY" -> "INSURANCE";
                        case "SAVINGS_MONTHLY" -> "SAVINGS";
                        default -> "EXPENSE";
                    };
            case "CARD" -> "CARD";
            default -> "EXPENSE";
        };
    }

    private String buildTitle(
            int year,
            String periodLabel,
            LedgerBook ledger,
            String category,
            String subView,
            ReportGranularity g) {
        String gran =
                switch (g) {
                    case MONTHLY -> "월간";
                    case WEEKLY -> "주간";
                    case DAILY -> "일간";
                };
        String cat =
                switch (category) {
                    case "INCOME" -> "수입";
                    case "SAVINGS_INSURANCE" -> "저축/보험";
                    case "LOAN" -> "대출";
                    case "EXPENSE" -> "지출";
                    case "CARD" -> "카드";
                    case "BUDGET" -> "예산/실적";
                    case "CALENDAR" -> "달력보기";
                    default -> "전체";
                };
        return gran + " 보고서 · " + year + "년 · " + ledger.label() + " · " + periodLabel + " · " + cat;
    }

    private List<String> footnotes(String category, String subView) {
        List<String> notes = new ArrayList<>();
        if ("ALL".equals(category) || "EXPENSE".equals(category) || "CALENDAR".equals(category)) {
            notes.add("현금지출 : 카드결제일의 현금으로 지출된 카드대금은 제외");
            notes.add("카드지출 : 일별 사용금액 기준");
        }
        if ("SAVINGS_INSURANCE".equals(category)) {
            notes.add("저축/보험은 「저축」 거래 중 분류에 「보험」 포함 여부로 구분합니다.");
        }
        if ("LOAN".equals(category) || "ALL".equals(category)) {
            notes.add("대출은 「지출」 거래 중 분류에 「대출」이 포함된 것입니다.");
        }
        if ("BUDGET".equals(category) && "BUDGET_EVAL".equals(subView)) {
            notes.add("실적(지출)은 대출 제외 순수 지출 합계입니다.");
        }
        return notes;
    }

    private boolean isDetailView(String category, String subView) {
        if ("DETAIL".equals(subView)) return true;
        if ("SAVINGS_DETAIL".equals(subView) || "INSURANCE_DETAIL".equals(subView)) return true;
        if ("CARD".equals(category) && "BY_PAYMENT_DATE".equals(subView)) return true;
        return false;
    }

    private boolean isCalendar(String category) {
        return "CALENDAR".equals(category);
    }

    private ReportGranularity resolveGranularity(
            String category, String subView, ReportGranularity granularity) {
        if ("EXPENSE".equals(category)) {
            return switch (subView) {
                case "WEEKLY" -> ReportGranularity.WEEKLY;
                case "DAILY" -> ReportGranularity.DAILY;
                default -> ReportGranularity.MONTHLY;
            };
        }
        if (isDetailView(category, subView) || isCalendar(category) || "BUDGET".equals(category)) {
            return ReportGranularity.MONTHLY;
        }
        return granularity;
    }

    private String defaultSubView(String category) {
        return switch (category) {
            case "INCOME", "LOAN" -> "MONTHLY";
            case "SAVINGS_INSURANCE" -> "ALL";
            case "EXPENSE" -> "MONTHLY";
            case "CARD" -> "MONTHLY";
            case "BUDGET" -> "BUDGET_EVAL";
            case "CALENDAR" -> "ALL";
            default -> "MONTHLY";
        };
    }

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String periodLabel(ReportPeriod period) {
        return switch (period) {
            case H1 -> "상반기";
            case H2 -> "하반기";
            case FY -> "전체";
        };
    }

    private static LocalDate periodStart(int year, ReportPeriod period) {
        return switch (period) {
            case H1 -> LocalDate.of(year, 1, 1);
            case H2 -> LocalDate.of(year, 7, 1);
            case FY -> LocalDate.of(year, 1, 1);
        };
    }

    private static LocalDate periodEnd(int year, ReportPeriod period) {
        return switch (period) {
            case H1 -> LocalDate.of(year, 6, 30);
            case H2 -> LocalDate.of(year, 12, 31);
            case FY -> LocalDate.of(year, 12, 31);
        };
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

    private static BigDecimal n(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private static long toLong(BigDecimal v) {
        return n(v).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static ReportPeriod parsePeriod(String raw) {
        if (raw == null || raw.isBlank()) return ReportPeriod.H1;
        try {
            return ReportPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ReportPeriod.H1;
        }
    }

    public static ReportGranularity parseGranularity(String raw) {
        if (raw == null || raw.isBlank()) return ReportGranularity.MONTHLY;
        try {
            return ReportGranularity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ReportGranularity.MONTHLY;
        }
    }

    private record TimeBucket(LocalDate start, LocalDate end, String label, String range) {}
}
